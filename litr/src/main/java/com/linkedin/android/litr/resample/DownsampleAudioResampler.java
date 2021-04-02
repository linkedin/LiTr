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
// from: https://github.com/natario1/Transcoder/blob/main/lib/src/main/java/com/otaliastudios/transcoder/resample/DownsampleAudioResampler.java
// modified: changed the signature of resample() method
// modified: small modification for "outputBuffer.put(inputBuffer.get());" logic
// modified: small modification for "inputBuffer.position(inputBuffer.position() + 1);" logic
package com.linkedin.android.litr.resample;

import android.media.MediaFormat;
import androidx.annotation.NonNull;
import java.nio.ByteBuffer;

/**
 * An {@link AudioResampler} that downsamples from a higher sample rate to a lower sample rate.
 */
public class DownsampleAudioResampler implements AudioResampler {

    private float ratio(int remaining, int all) {
        return (float) remaining / all;
    }

    @Override
    public void resample(@NonNull ByteBuffer inputBuffer, @NonNull ByteBuffer outputBuffer,
            @NonNull MediaFormat sourceMediaFormat, @NonNull MediaFormat targetMediaFormat) {
        int inputSampleRate = getSampleRate(sourceMediaFormat);
        int outputSampleRate = getSampleRate(targetMediaFormat);
        int channels = getChannels(targetMediaFormat);
        if (inputSampleRate < outputSampleRate) {
            throw new IllegalArgumentException("Illegal use of DownsampleAudioResampler");
        }
        if (channels != 1 && channels != 2) {
            throw new IllegalArgumentException("Illegal use of DownsampleAudioResampler. Channels:" + channels);
        }
        final int inputSamples = inputBuffer.remaining() / channels;
        final int outputSamples = (int) Math.ceil(inputSamples * ((double) outputSampleRate / inputSampleRate));
        final int dropSamples = inputSamples - outputSamples;
        int remainingOutputSamples = outputSamples;
        int remainingDropSamples = dropSamples;
        float remainingOutputSamplesRatio = ratio(remainingOutputSamples, outputSamples);
        float remainingDropSamplesRatio = ratio(remainingDropSamples, dropSamples);
        while (remainingOutputSamples > 0 || remainingDropSamples > 0) {
            // Will this be an input sample or a drop sample?
            // Choose the one with the bigger ratio.
            if (remainingOutputSamplesRatio >= remainingDropSamplesRatio) {
                for (int i = 0; i < channels; i++) {
                    outputBuffer.put(inputBuffer.get());
                }
                remainingOutputSamples--;
                remainingOutputSamplesRatio = ratio(remainingOutputSamples, outputSamples);
            } else {
                // Drop this - read from input without writing.
                for (int i = 0; i < channels; i++) {
                    inputBuffer.position(inputBuffer.position() + 1);
                }
                remainingDropSamples--;
                remainingDropSamplesRatio = ratio(remainingDropSamples, dropSamples);
            }
        }
    }
}
