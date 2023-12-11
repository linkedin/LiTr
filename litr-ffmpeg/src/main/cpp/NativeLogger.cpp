/*
 * Copyright 2023 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */

#include <jni.h>

#include "FFmpeg.h"
#include "Logging.h"

static void av_log_callback(void *ptr, int level, const char *fmt, va_list vl) {
    // Check to see if we care about the log.
    if (level > av_log_get_level())
        return;

    va_list vl2;
    char line[1024];
    static int print_prefix = 1;

    // Extract and format the log line.
    va_copy(vl2, vl);
    av_log_format_line(ptr, level, fmt, vl2, line, sizeof(line), &print_prefix);
    va_end(vl2);

    // Log it via Android with the relevant logcat equivalent level.
    if (level <= AV_LOG_ERROR) {
        LOGE("FFMPEG: %s", line);
    } else if (level <= AV_LOG_WARNING) {
        LOGW("FFMPEG: %s", line);
    } else if (level <= AV_LOG_INFO) {
        LOGI("FFMPEG: %s", line);
    } else {
        LOGD("FFMPEG: %s", line);
    }
}

extern "C" JNIEXPORT void
Java_com_linkedin_android_litr_muxers_NativeLogger_nativeSetup(
        JNIEnv *env,
        jobject /* this */,
        jint level
) {
    // Configure the log level and attach a callback.
    av_log_set_level(level);
    av_log_set_callback(av_log_callback);
}

