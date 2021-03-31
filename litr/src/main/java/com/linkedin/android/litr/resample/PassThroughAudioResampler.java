package com.linkedin.android.litr.resample;

import androidx.annotation.NonNull;
import java.nio.ByteBuffer;

/**
 * An {@link AudioResampler} that does nothing, meant to be used when sample
 * rates are identical.
 */
public class PassThroughAudioResampler implements AudioResampler {

    @Override
    public void resample(@NonNull ByteBuffer inputBuffer, int inputSampleRate,
                         @NonNull ByteBuffer outputBuffer, int outputSampleRate, int channels) {
        if (inputSampleRate != outputSampleRate) {
            throw new IllegalArgumentException("Illegal use of PassThroughAudioResampler");
        }
        outputBuffer.put(inputBuffer);
    }
}
