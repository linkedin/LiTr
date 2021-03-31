package com.linkedin.android.litr.resample;

import androidx.annotation.NonNull;
import java.nio.ByteBuffer;

/**
 * An {@link AudioResampler} that upsamples from a lower sample rate to a higher sample rate.
 */
public class UpsampleAudioResampler extends PassThroughAudioResampler {
    @Override
    public void resample(@NonNull ByteBuffer inputBuffer, int inputSampleRate, @NonNull ByteBuffer outputBuffer,
            int outputSampleRate, int channels) {
        super.resample(inputBuffer, inputSampleRate, outputBuffer, outputSampleRate, channels);
        // TODO currently up-sampling not supported because it's need some enhancements, so fallback to PASSTHROUGH approach
    }

    //    private static float ratio(int remaining, int all) {
    //        return (float) remaining / all;
    //    }
    //
    //    @Override
    //    public void resample(@NonNull ByteBuffer inputBuffer, int inputSampleRate, @NonNull ByteBuffer outputBuffer, int outputSampleRate, int channels) {
    //        if (inputSampleRate > outputSampleRate) {
    //            throw new IllegalArgumentException("Illegal use of UpsampleAudioResampler");
    //        }
    //        if (channels != 1 && channels != 2) {
    //            throw new IllegalArgumentException("Illegal use of UpsampleAudioResampler. Channels:" + channels);
    //        }
    //        final int inputSamples = inputBuffer.remaining() / channels;
    //        final int outputSamples = (int) Math.ceil(inputSamples * ((double) outputSampleRate / inputSampleRate));
    //        final int fakeSamples = outputSamples - inputSamples;
    //        int remainingInputSamples = inputSamples;
    //        int remainingFakeSamples = fakeSamples;
    //        float remainingInputSamplesRatio = ratio(remainingInputSamples, inputSamples);
    //        float remainingFakeSamplesRatio = ratio(remainingFakeSamples, fakeSamples);
    //        while (remainingInputSamples > 0 || remainingFakeSamples > 0) {
    //            // Will this be an input sample or a fake sample?
    //            // Choose the one with the bigger ratio.
    //            if (remainingInputSamplesRatio >= remainingFakeSamplesRatio) {
    //                for (int i = 0; i < channels; i++) {
    //                    outputBuffer.put(inputBuffer.get());
    //                }
    //                remainingInputSamples--;
    //                remainingInputSamplesRatio = ratio(remainingInputSamples, inputSamples);
    //            } else {
    //                for (int channel = 1; channel <= channels; channel++) {
    //                    outputBuffer.put(fakeSample(outputBuffer, inputBuffer, channel, channels));
    //                }
    //                remainingFakeSamples--;
    //                remainingFakeSamplesRatio = ratio(remainingFakeSamples, fakeSamples);
    //            }
    //        }
    //    }
    //
    //    /**
    //     * We have different options here.
    //     * 1. Return a 0 sample.
    //     * 2. Return a noise sample.
    //     * 3. Return the previous sample for this channel.
    //     * 4. Return an interpolated value between previous and next sample for this channel.
    //     *
    //     * None of this provides a real quality improvement, since the fundamental issue is that we
    //     * can not achieve a higher quality by simply inserting fake samples each X input samples.
    //     * A real upsampling algorithm should do something more intensive like interpolating between
    //     * all values, not just the spare one.
    //     *
    //     * However this is probably beyond the scope of this library.
    //     *
    //     * @param output output buffer
    //     * @param input output buffer
    //     * @param channel current channel
    //     * @param channels number of channels
    //     * @return a fake sample
    //     */
    //    @SuppressWarnings("unused")
    //    private static byte fakeSample(@NonNull ByteBuffer output, @NonNull ByteBuffer input, int channel, int channels) {
    //        return output.get(output.position() - channels);
    //    }
}
