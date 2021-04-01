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
// modified: removed UPSAMPLE field
package com.linkedin.android.litr.resample;

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
     * @param inputSampleRate the input sample rate
     * @param outputBuffer the output buffer
     * @param outputSampleRate the output sample rate
     * @param channels the number of channels
     */
    void resample(@NonNull final ByteBuffer inputBuffer, int inputSampleRate, @NonNull final ByteBuffer outputBuffer, int outputSampleRate, int channels);

    AudioResampler DOWNSAMPLE = new DownsampleAudioResampler();

    AudioResampler PASSTHROUGH = new PassThroughAudioResampler();
}
