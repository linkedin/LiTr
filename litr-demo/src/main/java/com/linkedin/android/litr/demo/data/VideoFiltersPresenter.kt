/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data

import android.content.Context
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TransformationOptions
import java.util.UUID

class VideoFiltersPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    fun applyFilter(
        sourceMedia: SourceMedia,
        targetMedia: TargetMedia,
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

        val transformationOptions = TransformationOptions.Builder()
            .setGranularity(MediaTransformer.GRANULARITY_DEFAULT)
            .setVideoFilters(listOf(targetMedia.filter))
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
