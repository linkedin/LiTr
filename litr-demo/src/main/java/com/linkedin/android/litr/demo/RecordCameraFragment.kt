/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */
package com.linkedin.android.litr.demo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.demo.data.TargetMedia
import com.linkedin.android.litr.demo.data.TransformationPresenter
import com.linkedin.android.litr.demo.data.TransformationState
import com.linkedin.android.litr.demo.databinding.FragmentCameraRecordBinding
import com.linkedin.android.litr.io.AudioRecordMediaSource
import com.linkedin.android.litr.io.ExternalMediaSource
import com.linkedin.android.litr.utils.TransformationUtil
import java.io.File
import java.util.*

private const val TAG = "RecordCameraFragment"

private const val REQUEST_AUDIO_AND_CAMERA_PERMISSION = 27

private const val DEFAULT_CAMERA_FPS = 30
private const val DEFAULT_TARGET_BITRATE = 5_000_000 // 5Mbps
private const val DEFAULT_RECORD_WIDTH = 1280

@RequiresApi(Build.VERSION_CODES.M)
class RecordCameraFragment : BaseTransformationFragment() {
    private lateinit var binding: FragmentCameraRecordBinding

    private lateinit var mediaTransformer: MediaTransformer
    private var targetMedia: TargetMedia = TargetMedia()

    private var surfaceHolder: SurfaceHolder? = null
    private var transformerTexture: Surface? = null
    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    private val surfaceHolderListener = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.i(TAG, "surfaceCreated")
            surfaceHolder = holder
        }

        override fun surfaceChanged(surfaceHolder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}
        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {}
    }

    private val cameraManager: CameraManager by lazy {
        requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val cameraId: String? by lazy {
        val cameraIds = cameraManager.cameraIdList
        for (id in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            if (characteristics.get(CameraCharacteristics.LENS_FACING)
                    == CameraCharacteristics.LENS_FACING_FRONT
            ) {
                continue
            }

            return@lazy id
        }

        return@lazy null
    }

    private var cameraDevice: CameraDevice? = null
    private var captureRequest: CaptureRequest? = null
    private var captureSession: CameraCaptureSession? = null

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            initCapture(device)
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

            val exception = RuntimeException("Camera $cameraId error: ($error) $msg")
            Log.e(TAG, exception.message, exception)

            throw exception
        }
    }

    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            captureRequest?.let {
                session.setRepeatingRequest(it, object : CaptureCallback() {
                    override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                    ) {
                        // Notify the MediaSource when a frame is captured.
                        binding.videoMediaSource?.onFrameAvailable()
                    }
                }, backgroundHandler)
            }
        }

        override fun onClosed(session: CameraCaptureSession) {
            stopBackgroundThread()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) { }
    }

    private val mediaSourceCallback = object : ExternalMediaSource.Callback {
        override fun onInputSurfaceAvailable(inputSurface: Surface) {
            transformerTexture = inputSurface
            cameraDevice?.let { startCapture(it) }
        }

        override fun onFrameSkipped(frameSkipCount: Int) {
            Log.e(TAG, "onFrameSkipped (Count: $frameSkipCount)")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaTransformer = MediaTransformer(context!!.applicationContext)

        // This demo fragment requires Android M or newer, in order to support reading data from
        // AudioRecord in a non-blocking way. Let's double check that the current device supports
        // this.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            AlertDialog.Builder(requireContext())
                    .setMessage("Android Marshmallow or newer required")
                    .setPositiveButton("Ok") { _, _ ->
                        activity?.onBackPressed()
                    }
                    .show()
            return
        }

        // Check to see what permission, if any, are required.
        val requiredPermissions = mutableListOf<String>()
        if (!hasAudioRecordPermission()) {
            requiredPermissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!hasCameraPermission()) {
            requiredPermissions.add(Manifest.permission.CAMERA)
        }

        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                    context as Activity,
                    requiredPermissions.toTypedArray(),
                    REQUEST_AUDIO_AND_CAMERA_PERMISSION
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaTransformer.release()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraRecordBinding.inflate(layoutInflater, container, false)

        binding.cameraView.holder.addCallback(surfaceHolderListener)
        binding.buttonRecord.setOnClickListener { startRecording() }
        binding.buttonStop.setOnClickListener { stopRecording() }

        binding.transformationState = TransformationState()
        binding.transformationPresenter = TransformationPresenter(context!!, mediaTransformer)
        binding.audioMediaSource = AudioRecordMediaSource()
        binding.videoMediaSource = ExternalMediaSource(mediaSourceCallback)

        val targetFile = File(
                TransformationUtil.getTargetFileDirectory(requireContext().applicationContext),
                "recorded_camera_${System.currentTimeMillis()}.mp4"
        )
        targetMedia.setTargetFile(targetFile)
        binding.targetMedia = targetMedia

        return binding.root
    }

    private fun startRecording() {
        val id = cameraId ?: return

        if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED)
        {
            startBackgroundThread()
            cameraManager.openCamera(id, cameraStateCallback, backgroundHandler)
        }
    }

    private fun initCapture(camera: CameraDevice) {
        cameraDevice = camera

        // Select the appropriate preview size and configure the Camera View.
        val characteristics = cameraManager.getCameraCharacteristics(camera.id)
        val sensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val previewSize = getPreviewOutputSize(
                binding.cameraView.display,
                characteristics,
                SurfaceHolder::class.java)
        Log.i(TAG, "View finder size: ${binding.cameraView.width} x ${binding.cameraView.height}")
        Log.i(TAG, "Selected preview size: $previewSize")

        binding.cameraView.post {
            binding.cameraView.setAspectRatio(previewSize.width, previewSize.height)
        }

        val recordSize = getRecordSize(sensorRect)
        binding.videoMediaSource?.apply {
            width = recordSize.width
            height = recordSize.height
            bitrate = DEFAULT_TARGET_BITRATE
            frameRate = DEFAULT_CAMERA_FPS
            orientation = 0
        }

        binding.transformationPresenter?.recordCamera(
                binding.audioMediaSource!!,
                binding.videoMediaSource!!,
                binding.targetMedia!!,
                binding.transformationState!!
        )
    }

    private fun startCapture(camera: CameraDevice) {
        val surfaces = mutableListOf<Surface>()
        captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            surfaceHolder?.let {
                addTarget(it.surface)
                surfaces.add(it.surface)
            }
            transformerTexture?.let {
                addTarget(it)
                surfaces.add(it)
            }

            // Set a fixed target frame rate. This will match what we'll assume we are encoding too.
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(DEFAULT_CAMERA_FPS, DEFAULT_CAMERA_FPS))
        }.build()

        camera.createCaptureSession(surfaces, captureStateCallback, backgroundHandler)
    }

    private fun stopRecording() {
        binding.transformationPresenter?.stopRecording(
                binding.audioMediaSource!!,
                binding.videoMediaSource!!
        )

        captureSession?.apply {
            stopRepeating()
            close()
        }
    }

    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("CameraVideoThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundHandlerThread.quitSafely()
        backgroundHandlerThread.join()
    }

    private fun hasAudioRecordPermission(): Boolean {
        val validContext = context ?: return false
        return ContextCompat.checkSelfPermission(
                validContext,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCameraPermission(): Boolean {
        val validContext = context ?: return false
        return ContextCompat.checkSelfPermission(
                validContext,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Since we don't support cropping of the sensor or input surface, we will record to a Surface
     * that has the same aspect ratio as the camera's sensor. We will target something similar to
     * 720p (1280 x 720) but allow the height to increase when required for non-16:9 ratios.
     */
    private fun getRecordSize(sensorRect: Rect?): Size {
        val aspectRatio = sensorRect?.let { it.height().toFloat() / it.width().toFloat() } ?: (3f / 4f)

        val width = DEFAULT_RECORD_WIDTH

        // Compute a suitable height of Surface, ensuring that we always generate something that is
        // dividable by 4. This ensures a supported alignment.
        var height = (width * aspectRatio).toInt()
        height -= height % 4

        return if (isLandscape()) {
            Size(width, height)
        } else {
            Size(height, width)
        }
    }

    /**
     * Returns whether or not the device is currently in a landscape orientation.
     */
    private fun isLandscape() = resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
}
