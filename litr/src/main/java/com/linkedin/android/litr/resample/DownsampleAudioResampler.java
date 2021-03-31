package com.linkedin.android.litr.resample;

import androidx.annotation.NonNull;
import java.nio.ByteBuffer;

/**
 * An {@link AudioResampler} that downsamples from a higher sample rate to a lower sample rate.
 */
public class DownsampleAudioResampler implements AudioResampler {

    private static float ratio(int remaining, int all) {
        return (float) remaining / all;
    }

    @Override
    public void resample(@NonNull ByteBuffer inputBuffer, int inputSampleRate, @NonNull ByteBuffer outputBuffer, int outputSampleRate, int channels) {
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
