/*
 * Copyright (C) 2021 natario1 Transcoder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// from: https://github.com/natario1/Transcoder/blob/main/lib/src/main/java/com/otaliastudios/transcoder/resample/AudioResampler.java
// modified: removed all constant fields from this interface
// modified: changed the signature of resample() method
// modified: added two other default methods, getSampleRate() and getChannels()
package com.linkedin.android.litr.resample;

import android.media.MediaFormat;
import androidx.annotation.NonNull;
import java.nio.ByteBuffer;

/**
 * Resamples audio data. See {@link DownsampleAudioResampler} for concrete implementations.
 */
public interface AudioResampler {

    /**
     * Resamples input audio from input buffer into the output buffer.
     *
     * @param inputBuffer the input buffer
     * @param outputBuffer the output buffer
     * @param sourceMediaFormat the source media format
     * @param targetMediaFormat the target media format
     */
    void resample(@NonNull final ByteBuffer inputBuffer, @NonNull final ByteBuffer outputBuffer,
            @NonNull MediaFormat sourceMediaFormat, @NonNull MediaFormat targetMediaFormat);

    /**
     * Return the sample rate of an audio format
     */
    default int getSampleRate(MediaFormat format) {
        return format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
    }

    /**
     * Return the width of the content in a video format.
     */
    default int getChannels(MediaFormat format) {
        return format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
    }
}
