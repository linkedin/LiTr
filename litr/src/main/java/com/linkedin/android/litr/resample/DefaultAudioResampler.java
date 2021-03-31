package com.linkedin.android.litr.resample;

import androidx.annotation.NonNull;
import java.nio.ByteBuffer;

/**
 * An {@link AudioResampler} that delegates to appropriate classes
 * based on input and output size.
 */
public class DefaultAudioResampler implements AudioResampler {

    @Override
    public void resample(@NonNull ByteBuffer inputBuffer, int inputSampleRate, @NonNull ByteBuffer outputBuffer, int outputSampleRate, int channels) {
        if (inputSampleRate < outputSampleRate) {
            UPSAMPLE.resample(inputBuffer, inputSampleRate, outputBuffer, outputSampleRate, channels);
        } else if (inputSampleRate > outputSampleRate) {
            DOWNSAMPLE.resample(inputBuffer, inputSampleRate, outputBuffer, outputSampleRate, channels);
        } else {
            PASSTHROUGH.resample(inputBuffer, inputSampleRate, outputBuffer, outputSampleRate, channels);
        }
    }
}
