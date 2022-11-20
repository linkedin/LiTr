package com.linkedin.android.litr.io

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

private const val TAG = "Camera2MediaSource"

private const val DEFAULT_RECORD_WIDTH = 1280

/**
 * An implementation of MediaSource, which utilises Android's Camera2 APIs to capture from a
 * CameraDevice and render to the CaptureMediaSource's input Surface. An instance of this class is
 * expected to be both the MediaSource, and Decoder for which the pipeline is configured. This
 * allows these components to be bypassed.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2MediaSource(
        context: Context,
        private val cameraId: String
): CaptureMediaSource(), CaptureMediaSource.Callback {
    /**
     * Callback which notifies when the device is ready, as well as other details regarding the
     * capture session.
     */
    interface Callback {
        fun onDeviceReady(cameraCharacteristics: CameraCharacteristics)
        fun onFrameSkipped(frameSkipCount: Int)

        fun onError(exception: Exception)
    }

    var recordWidth = DEFAULT_RECORD_WIDTH

    private var callback: Callback? = null
    private var inputSurface: Surface? = null

    // We support an optional SurfaceHolder that can be used as a Preview for the Camera capture.
    private var previewSurfaceHolder: SurfaceHolder? = null
    private val previewSurfaceHolderListener = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.i(TAG, "Preview Surface Created")
            previewSurfaceHolder = holder
        }

        override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {}
    }

    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureRequest: CaptureRequest? = null
    private var captureSession: CameraCaptureSession? = null

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            initDevice(device)
        }

        override fun onDisconnected(device: CameraDevice) { }
        override fun onError(device: CameraDevice, error: Int) {
            val msg = when(error) {
                ERROR_CAMERA_DEVICE -> "Fatal (device)"
                ERROR_CAMERA_DISABLED -> "Device policy"
                ERROR_CAMERA_IN_USE -> "Camera in use"
                ERROR_CAMERA_SERVICE -> "Fatal (service)"
                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                else -> "Unknown"
            }

            // Notify via the Callback that an error occurred.
            val exception = RuntimeException("Camera $cameraId error: ($error) $msg")
            Log.e(TAG, exception.message, exception)
            callback?.onError(exception)
        }
    }

    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            captureRequest?.let {
                session.setRepeatingRequest(it, object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                    ) {
                        // Notify the CaptureMediaSource when a frame is captured.
                        onFrameAvailable()
                    }
                }, backgroundHandler)
            }
        }

        override fun onClosed(session: CameraCaptureSession) {
            // When the session has been cleanly closed, we can go ahead and stop our background
            // Thread.
            stopBackgroundThread()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) { }
    }

    init {
        // Add ourselves as a callback to our parent CaptureMediaSource. This will allow us to
        // determine when an input Surface is available to draw too via the Camera.
        setCallback(this)
    }

    /**
     * Sets the callback that will be notified on the state of our recording session.
     */
    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    /**
     * Adds an optional SurfaceHolder that can be used as a Preview surface.
     */
    fun addPreviewSurfaceHolder(surfaceHolder: SurfaceHolder) {
        surfaceHolder.addCallback(previewSurfaceHolderListener)
    }

    /**
     * Opens the configured CameraDevice so that it's ready for recording. Once available, the
     * configured Callback will be notified via onDeviceReady. Once this has done, the consumer is
     * able to start the Transcode session.
     */
    @RequiresPermission("android.permission.CAMERA")
    @Synchronized
    fun openCamera() {
        startBackgroundThread()
        cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
    }

    /**
     * Stops any in-progress recording, allowing the MediaSource to notify the end of the video
     * stream. It will also allow any internal resources to be cleaned up.
     */
    @Synchronized
    fun stopRecording() {
        stopExternal()

        captureSession?.apply {
            stopRepeating()
            close()
        }
    }

    /**
     * Once a device has been opened, we can query it's Characteristics to know how to configure
     * the session.
     */
    private fun initDevice(camera: CameraDevice) {
        cameraDevice = camera

        val characteristics = cameraManager.getCameraCharacteristics(camera.id)
        val sensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

        val recordSize = getRecordSize(sensorRect, sensorOrientation)
        width = recordSize.width
        height = recordSize.height

        callback?.onDeviceReady(characteristics)
    }

    /**
     * Starts the capture session for the given CameraDevice. We will configure the input Surface
     * provided by the CaptureMediaSource, as well as the optional Preview SurfaceHolder.
     */
    private fun startCaptureSession(camera: CameraDevice) {
        val surfaces = mutableListOf<Surface>()
        captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            // Add the (optional) preview SurfaceHolder.
            previewSurfaceHolder?.let {
                addTarget(it.surface)
                surfaces.add(it.surface)
            }

            // Add the CaptureMediaSource's input Surface.
            inputSurface?.let {
                addTarget(it)
                surfaces.add(it)
            }

            // Set a fixed target frame rate. This will match what we'll assume we are encoding too.
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(frameRate, frameRate))
        }.build()

        camera.createCaptureSession(surfaces, captureStateCallback, backgroundHandler)
    }

    /**
     * Since we don't support cropping of the sensor or input surface, we will record to a Surface
     * that has the same aspect ratio as the camera's sensor.
     */
    private fun getRecordSize(sensorRect: Rect?, sensorOrientation: Int?): Size {
        val aspectRatio = sensorRect?.let { it.height().toFloat() / it.width().toFloat() } ?: (3f / 4f)
        val orientation = sensorOrientation ?: 0

        val width = recordWidth

        // Compute a suitable height of Surface, ensuring that we always generate something that is
        // dividable by 4. This ensures a supported alignment.
        var height = (width * aspectRatio).toInt()
        height -= height % 4

        return if (orientation == 0 || orientation == 180) {
            Size(width, height)
        } else {
            Size(height, width)
        }
    }

    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("Camera2VideoThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundHandlerThread.quitSafely()
        backgroundHandlerThread.join()
    }

    //region CaptureMediaSource...

    override fun onInputSurfaceAvailable(surface: Surface) {
        inputSurface = surface
        cameraDevice?.let { startCaptureSession(it) }
    }

    override fun onFrameSkipped(frameSkipCount: Int) {
        callback?.onFrameSkipped(frameSkipCount)
    }

    //endregion
}