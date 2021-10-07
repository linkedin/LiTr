package com.linkedin.android.litr.demo.camera

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import com.linkedin.android.litr.utils.getPreviewOutputSize
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class MultiTargetCameraThread @JvmOverloads constructor(
    private val cameraManager: CameraManager,
    private val listener: CameraThreadListener?,
    private val previewSurface: Surface,
    private val recordSurfaceTexture: SurfaceTexture? = null
) : Thread() {
    private val threadLock = ReentrantLock()
    private val threadLockCondition = threadLock.newCondition()
    private var cameraHandler: CameraHandler? = null
    private var cameraDevice: CameraDevice? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var sensorArraySize: Rect? = null
    private var cameraSize: Size? = null

    var isRunning = false
        private set

    fun getHandler(): CameraHandler? {
        threadLock.withLock {
            try {
                // Wait until thread is initialized (and handler is set)
                threadLockCondition.await()
            } catch (e: InterruptedException) {
                Log.w(TAG, "Failed to get camera handler: interrupted while waiting for thread to initialize", e)
            }
        }
        return cameraHandler
    }


    override fun run() {
        super.run()

        Looper.prepare()

        threadLock.withLock {
            val looper = Looper.myLooper()!!
            cameraHandler = CameraHandler(this, looper)
            isRunning = true
            threadLockCondition.signalAll()
        }
        Looper.loop()
        listener?.onCameraStopped()
        threadLock.withLock {
            cameraHandler = null
            isRunning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startPreview(preferredWidth: Int, preferredHeight: Int) {
        cameraManager.cameraIdList.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val lensFacing = characteristics[CameraCharacteristics.LENS_FACING]
            val sensorOrientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION]

            if (lensFacing == null || sensorOrientation == null) {
                // Try a different camera
                return@forEach
            }

            if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

                cameraSize = getPreviewOutputSize(preferredWidth, preferredHeight, characteristics, SurfaceTexture::class.java)?.also {
                    recordSurfaceTexture?.setDefaultBufferSize(it.width, it.height)
                }

                val thread = HandlerThread("OpenCamera")
                thread.start()
                val backgroundHandler = Handler(thread.looper)

                cameraManager.openCamera(cameraId, cameraDeviceCallback, backgroundHandler)

                // TODO: Handle camera not found
                return
            }
        }
    }

    fun stopPreview() {
        captureRequestBuilder?.build()?.let { request ->
            cameraCaptureSession?.setRepeatingRequest(request, null, null)
            cameraDevice?.close()
        }
    }

    private val cameraDeviceCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {

            val surfaceSize = cameraSize ?: run {
                Log.e(TAG, "Attempted to open camera before camera size is known")
                return
            }

            cameraDevice = device

            val recordSurface = Surface(recordSurfaceTexture)

            // Create capture request for recording
            // TODO: Can throw CameraAccessException
            cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)?.let {
                it.addTarget(recordSurface)
                it.addTarget(previewSurface)
                captureRequestBuilder = it
            }

            val targets = listOf(recordSurface, previewSurface)

            device.createCaptureSession(targets, cameraCaptureStateCallback, cameraHandler)

            listener?.onCameraStarted(surfaceSize.width, surfaceSize.height)
        }

        override fun onDisconnected(device: CameraDevice) {
            Log.w(TAG, "Camera device ${device.id} has been disconnected")
        }

        override fun onError(device: CameraDevice, error: Int) {
            val msg = when (error) {
                ERROR_CAMERA_DEVICE -> "Fatal (device)"
                ERROR_CAMERA_DISABLED -> "Device policy"
                ERROR_CAMERA_IN_USE -> "Camera in use"
                ERROR_CAMERA_SERVICE -> "Fatal (service)"
                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                else -> "Unknown"
            }
            val exc = RuntimeException("Camera device ${device.id} error: ($error) $msg")
            Log.e(TAG, exc.message, exc)
        }
    }

    private val cameraCaptureStateCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session

            val captureRequest = captureRequestBuilder?.let { builder ->
                builder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
                builder.build()
            } ?: run {
                Log.e(TAG, "Attempting to set capture request but the builder is not available")
                return
            }

            val thread = HandlerThread("CameraPreview")
            thread.start()
            val backgroundHandler = Handler(thread.looper)

            // TODO: Can throw CameraAccessException
            session.setRepeatingRequest(captureRequest, null, backgroundHandler)
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            val exc = RuntimeException("Camera ${session.device.id} session configuration failed")
            Log.e(TAG, exc.message, exc)
        }
    }

    companion object {
        private val TAG = MultiTargetCameraThread::class.qualifiedName
    }
}