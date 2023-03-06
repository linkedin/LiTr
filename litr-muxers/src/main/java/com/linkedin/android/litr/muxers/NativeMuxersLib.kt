/*
 * Copyright 2023 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */
package com.linkedin.android.litr.muxers

import android.util.Log

private const val TAG = "NativeMuxersLib"
private val FFMPEG_LIBRARIES = listOf(
        "avutil",
        "avcodec",
        "avformat",
        "litr-muxers"
)

/**
 * Helper method for loading all required ffmpeg binaries and dependencies.
 */
object NativeMuxersLib {
    /**
     * Loads all required libraries for the Native Muxer.
     */
    fun loadLibraries() {
        FFMPEG_LIBRARIES.forEach {
            loadLibrary(it)
        }
    }

    private fun loadLibrary(libraryName: String) {
        try {
            System.loadLibrary(libraryName)
            Log.i(TAG, "Loaded: lib$libraryName")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Unable to load: lib$libraryName")
        }
    }
}