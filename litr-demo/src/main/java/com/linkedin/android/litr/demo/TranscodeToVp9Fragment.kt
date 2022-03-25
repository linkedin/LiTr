/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.demo.data.SourceMedia
import com.linkedin.android.litr.demo.data.TargetMedia
import com.linkedin.android.litr.demo.data.TranscodingConfigPresenter
import com.linkedin.android.litr.demo.data.TransformationPresenter
import com.linkedin.android.litr.demo.data.TransformationState
import com.linkedin.android.litr.demo.data.TrimConfig
import com.linkedin.android.litr.demo.databinding.FragmentVideoVp9Binding
import com.linkedin.android.litr.utils.TransformationUtil
import java.io.File

class TranscodeToVp9Fragment : BaseTransformationFragment(), MediaPickerListener {

    private lateinit var binding: FragmentVideoVp9Binding
    private lateinit var mediaTransformer: MediaTransformer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaTransformer = MediaTransformer(context!!.applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaTransformer.release()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoVp9Binding.inflate(inflater, container, false)

        val sourceMedia = SourceMedia()
        binding.sourceMedia = sourceMedia
        binding.sectionPickVideo.buttonPickVideo.setOnClickListener { pickVideo(this@TranscodeToVp9Fragment) }
        binding.transformationState = TransformationState()
        binding.transformationPresenter = TransformationPresenter(context!!, mediaTransformer)

        val targetMedia = TargetMedia()
        val transcodingConfigPresenter = TranscodingConfigPresenter(this, targetMedia)
        binding.transcodingConfigPresenter = transcodingConfigPresenter
        binding.targetMedia = targetMedia
        binding.trimConfig = TrimConfig()

        return binding.root
    }

    override fun onMediaPicked(uri: Uri) {
        binding.sourceMedia?.let { sourceMedia ->
            updateSourceMedia(sourceMedia, uri)
            binding.trimConfig?.let { trimConfig -> updateTrimConfig(trimConfig, sourceMedia) }
            val targetFile = File(TransformationUtil.getTargetFileDirectory(requireContext().applicationContext),
                "transcoded_" + TransformationUtil.getDisplayName(context!!, sourceMedia.uri))
            binding.targetMedia?.setTargetFile(targetFile)
            binding.targetMedia?.setTracks(sourceMedia.tracks)
            binding.transformationState?.setState(TransformationState.STATE_IDLE)
            binding.transformationState?.setStats(null)
        }
    }
}
