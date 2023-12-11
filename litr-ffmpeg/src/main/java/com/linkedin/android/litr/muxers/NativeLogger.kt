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

/**
 * Allows configuration of the logging performed by the native components.
 */
object NativeLogger {
    // Level's defined in avutil/log.h
    const val LEVEL_TRACE = 56
    const val LEVEL_DEBUG = 48
    const val LEVEL_VERBOSE = 40
    const val LEVEL_INFO = 32
    const val LEVEL_WARNING = 24
    const val LEVEL_ERROR = 16
    const val LEVEL_FATAL = 8
    const val LEVEL_PANIC = 0
    const val LEVEL_QUIET = -8

    /**
     * Configures the native logger with the given level. These log messages will be written to
     * the application's logcat messages.
     */
    fun setup(level: Int) = nativeSetup(level)

    private external fun nativeSetup(level: Int)
}