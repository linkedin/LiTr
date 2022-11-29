/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */
package com.linkedin.android.litr.muxers

import android.media.MediaMuxer

/**
 * This object contains the known (and supported) output formats for the NativeMediaMuxer.
 */
object NativeOutputFormats {
    const val FORMAT_MPEG4 = "mp4"
    const val FORMAT_MKV = "matroska"

    const val FORMAT_SEGMENT = "stream_segment"

    /**
     * Converts constants defined by MediaMuxer.OutputFormat into their equivalent NativeMediaMuxer
     * constant.
     */
    fun fromOutputFormat(outputFormat: Int): String {
        return when (outputFormat) {
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 -> FORMAT_MPEG4
            MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM -> FORMAT_MKV
            else -> error("Unsupported output format")
        }
    }
}