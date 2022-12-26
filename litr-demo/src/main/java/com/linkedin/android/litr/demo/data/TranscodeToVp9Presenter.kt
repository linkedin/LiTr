/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data

import android.content.Context
import android.media.MediaFormat
import android.os.Build
import android.widget.Toast
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TransformationOptions
import com.linkedin.android.litr.demo.R
import com.linkedin.android.litr.io.MediaRange
import java.util.UUID
import java.util.concurrent.TimeUnit

class TranscodeToVp9Presenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    fun transcodeToVp9(
        sourceMedia: SourceMedia,
        targetMedia: TargetMedia,
        trimConfig: TrimConfig,
        transformationState: TransformationState
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(context, R.string.error_vp9_not_supported, Toast.LENGTH_SHORT).show()
            return
        }

        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete()
        }

        transformationState.requestId = UUID.randomUUID().toString()
        val transformationListener = MediaTransformationListener(
            context,
            transformationState.requestId,
            transformationState,
            targetMedia
        )

        val mediaRange = if (trimConfig.enabled) MediaRange(
            TimeUnit.MILLISECONDS.toMicros((trimConfig.range[0] * 1000).toLong()),
            TimeUnit.MILLISECONDS.toMicros((trimConfig.range[1] * 1000).toLong())
        ) else MediaRange(0, Long.MAX_VALUE)

        val transformationOptions = TransformationOptions.Builder()
            .setGranularity(MediaTransformer.GRANULARITY_DEFAULT)
            .setSourceMediaRange(mediaRange)
            .setRemoveMetadata(true)
            .build()

        val targetVideoFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_VP9,
            1280,
            720
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 5_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
        }

        mediaTransformer.transform(
            transformationState.requestId,
            sourceMedia.uri,
            targetMedia.targetFile.path,
            targetVideoFormat,
            null,
            transformationListener,
            transformationOptions
        )
    }
}
