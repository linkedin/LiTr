/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */
package com.linkedin.android.litr.demo.fragment

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.demo.BaseTransformationFragment
import com.linkedin.android.litr.demo.R
import com.linkedin.android.litr.demo.data.RecordAudioPresenter
import com.linkedin.android.litr.demo.data.TargetMedia
import com.linkedin.android.litr.demo.data.TransformationState
import com.linkedin.android.litr.demo.databinding.FragmentAudioRecordBinding
import com.linkedin.android.litr.io.AudioRecordMediaSource
import com.linkedin.android.litr.utils.TransformationUtil
import java.io.File

private const val REQUEST_AUDIO_RECORD_PERMISSION = 14

class RecordAudioFragment : BaseTransformationFragment() {
    private lateinit var binding: FragmentAudioRecordBinding

    private lateinit var mediaTransformer: MediaTransformer
    private var targetMedia: TargetMedia = TargetMedia()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Check to make sure the user has granted permission to record audio.
        if (!hasAudioRecordPermission()) {
            ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_AUDIO_RECORD_PERMISSION
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaTransformer.release()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentAudioRecordBinding.inflate(layoutInflater, container, false)

        binding.transformationState = TransformationState()
        binding.transformationPresenter = RecordAudioPresenter(context!!, mediaTransformer)
        binding.mediaSource = AudioRecordMediaSource()

        val targetFile = File(
                TransformationUtil.getTargetFileDirectory(requireContext().applicationContext),
                "recorded_audio_${System.currentTimeMillis()}.mp4"
        )
        targetMedia.setTargetFile(targetFile)
        binding.targetMedia = targetMedia

        return binding.root
    }

    private fun hasAudioRecordPermission(): Boolean {
        val validContext = context ?: return false
        return ContextCompat.checkSelfPermission(
                validContext,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}