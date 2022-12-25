/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data

import android.content.Context
import android.graphics.PointF
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TransformationOptions
import com.linkedin.android.litr.io.MediaRange
import java.util.UUID
import java.util.concurrent.TimeUnit

class VideoWatermarkPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    fun applyWatermark(
        sourceMedia: SourceMedia,
        targetMedia: TargetMedia,
        trimConfig: TrimConfig,
        transformationState: TransformationState
    ) {
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

        val watermarkImageFilter = targetMedia.tracks.firstOrNull {it is TargetVideoTrack}?.let {
            createGlFilters(
                sourceMedia,
                it as TargetVideoTrack,
                0.2f,
                PointF(0.8f, 0.8f),
                0f
            )
        }

        val mediaRange = if (trimConfig.enabled) {
            MediaRange(
                TimeUnit.MILLISECONDS.toMicros((trimConfig.range[0] * 1000).toLong()),
                TimeUnit.MILLISECONDS.toMicros((trimConfig.range[1] * 1000).toLong())
            )
        } else {
            MediaRange(0, Long.MAX_VALUE)
        }

        val transformationOptions = TransformationOptions.Builder()
            .setGranularity(MediaTransformer.GRANULARITY_DEFAULT)
            .setVideoFilters(watermarkImageFilter)
            .setSourceMediaRange(mediaRange)
            .build()

        mediaTransformer.transform(
            transformationState.requestId,
            sourceMedia.uri,
            targetMedia.targetFile.path,
            null,
            null,
            transformationListener,
            transformationOptions
        )
    }
}
