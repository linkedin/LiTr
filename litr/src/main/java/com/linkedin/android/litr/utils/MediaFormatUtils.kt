package com.linkedin.android.litr.utils

import android.media.MediaFormat
import android.os.Build

class MediaFormatUtils {
    companion object {
        @JvmStatic
        fun getIFrameInterval(format: MediaFormat, defaultValue: Number): Number {
            return getNumber(format, MediaFormat.KEY_I_FRAME_INTERVAL) ?: defaultValue
        }

        @JvmStatic
        fun getFrameRate(format: MediaFormat, defaultValue: Number): Number {
            return getNumber(format, MediaFormat.KEY_FRAME_RATE) ?: defaultValue
        }

        @JvmStatic
        fun getChannelCount(format: MediaFormat, defaultValue: Number): Number {
            return getNumber(format, MediaFormat.KEY_CHANNEL_COUNT) ?: defaultValue
        }

        @JvmStatic
        fun getSampleRate(format: MediaFormat, defaultValue: Number): Number {
            return getNumber(format, MediaFormat.KEY_SAMPLE_RATE) ?: defaultValue
        }

        @JvmStatic
        fun getNumber(format: MediaFormat, key: String): Number? {
            return when {
                !format.containsKey(key) -> null
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> format.getNumber(key)
                else -> runCatching {
                    format.getInteger(key)
                }.recoverCatching {
                    format.getFloat(key)
                }.getOrNull()
            }
        }
    }
}