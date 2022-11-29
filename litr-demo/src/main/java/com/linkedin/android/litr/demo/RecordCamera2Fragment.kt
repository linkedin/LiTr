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
import android.hardware.camera2.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.demo.data.RecordCameraPresenter
import com.linkedin.android.litr.demo.data.TargetMedia
import com.linkedin.android.litr.demo.data.TransformationState
import com.linkedin.android.litr.demo.databinding.FragmentCamera2RecordBinding
import com.linkedin.android.litr.io.AudioRecordMediaSource
import com.linkedin.android.litr.io.Camera2MediaSource
import com.linkedin.android.litr.utils.TransformationUtil
import java.io.File
import java.util.*

private const val TAG = "RecordCamera2Fragment"

private const val REQUEST_AUDIO_AND_CAMERA_PERMISSION = 27

private const val DEFAULT_CAMERA_FPS = 30
private const val DEFAULT_TARGET_BITRATE = 5_000_000 // 5Mbps
private const val DEFAULT_RECORD_WIDTH = 1280

@RequiresApi(Build.VERSION_CODES.M)
open class RecordCamera2Fragment : BaseTransformationFragment() {
    protected lateinit var binding: FragmentCamera2RecordBinding

    private lateinit var mediaTransformer: MediaTransformer
    private var targetMedia: TargetMedia = TargetMedia()

    private lateinit var cameraManager: CameraManager

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

    private val mediaSourceCallback = object : Camera2MediaSource.Callback {
        override fun onDeviceReady(cameraCharacteristics: CameraCharacteristics) {
            initPreview(cameraCharacteristics)

            binding.transformationPresenter?.recordCamera(
                    binding.audioMediaSource!!,
                    binding.videoMediaSource!!,
                    binding.targetMedia!!,
                    binding.transformationState!!,
                    binding.enableNativeMuxer == true
            )
        }

        override fun onFrameSkipped(frameSkipCount: Int) {
            Log.e(TAG, "onFrameSkipped (Count: $frameSkipCount)")
        }

        override fun onError(exception: Exception) {
            Log.e(TAG, "onError $exception")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mediaTransformer = MediaTransformer(context!!.applicationContext)

        // This demo fragment requires Android M or newer, in order to support reading data from
        // AudioRecord in a non-blocking way. Let's double check that the current device supports
        // this.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            AlertDialog.Builder(requireContext())
                    .setMessage(R.string.error_marshmallow_or_newer_required)
                    .setPositiveButton(R.string.ok) { _, _ ->
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
        binding = FragmentCamera2RecordBinding.inflate(layoutInflater, container, false)

        binding.buttonRecord.setOnClickListener { startRecording() }
        binding.buttonStop.setOnClickListener { stopRecording() }

        binding.transformationState = TransformationState()
        binding.transformationPresenter = RecordCameraPresenter(context!!, mediaTransformer)
        binding.audioMediaSource = AudioRecordMediaSource()

        binding.videoMediaSource = Camera2MediaSource(requireContext(), cameraId!!).apply {
            frameRate = DEFAULT_CAMERA_FPS
            bitrate = DEFAULT_TARGET_BITRATE
            recordWidth = DEFAULT_RECORD_WIDTH

            setCallback(mediaSourceCallback)
            addPreviewSurfaceHolder(binding.cameraView.holder)
        }

        val targetFile = File(
                TransformationUtil.getTargetFileDirectory(requireContext().applicationContext),
                "recorded_camera_${System.currentTimeMillis()}.mp4"
        )
        targetMedia.setTargetFile(targetFile)
        binding.targetMedia = targetMedia

        return binding.root
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED)
        {
            binding.videoMediaSource?.openCamera()
        }
    }

    private fun initPreview(characteristics: CameraCharacteristics) {
        val previewSize = getPreviewOutputSize(
                binding.cameraView.display,
                characteristics,
                SurfaceHolder::class.java)
        Log.i(TAG, "View finder size: ${binding.cameraView.width} x ${binding.cameraView.height}")
        Log.i(TAG, "Selected preview size: $previewSize")

        binding.cameraView.post {
            binding.cameraView.setAspectRatio(previewSize.width, previewSize.height)
        }
    }

    private fun stopRecording() {
        binding.transformationPresenter?.stopRecording(
                binding.audioMediaSource!!,
                binding.videoMediaSource!!
        )
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
}
