/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
#include <jni.h>
#include "oboe_resampler/MultiChannelResampler.h"

using namespace resampler;

MultiChannelResampler* oboeResampler = nullptr;
int inputChannelCount = -1;
int ouputChannelCount = -1;

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_render_OboeAudioResampler_initResampler(
        JNIEnv* env,
        jobject /* this */,
        jint sourceChannelCount,
        jint sourceSampleRate,
        jint targetChannelCount,
        jint targetSampleRate) {
    oboeResampler = MultiChannelResampler::make(
            targetChannelCount,
            sourceSampleRate,
            targetSampleRate,
            MultiChannelResampler::Quality::High);
    inputChannelCount = sourceChannelCount;
    ouputChannelCount = targetChannelCount;
}

extern "C" JNIEXPORT int JNICALL
Java_com_linkedin_android_litr_render_OboeAudioResampler_resample(
        JNIEnv* env,
        jobject,
        jobject jsourceBuffer,
        jint sampleCount,
        jobject jtargetBuffer) {
    if (oboeResampler != nullptr && inputChannelCount > 0 && ouputChannelCount > 0) {
        auto sourceBuffer = (jbyte *) env->GetDirectBufferAddress(jsourceBuffer);
        auto targetBuffer = (jbyte *) env->GetDirectBufferAddress(jtargetBuffer);

        auto resamplerInputBuffer = new float[ouputChannelCount];
        auto resamplerOutputBuffer = new float[ouputChannelCount];

        int framesProcessed = 0;
        int inputFramesLeft = sampleCount;

        while (inputFramesLeft > 0) {
            if (oboeResampler->isWriteNeeded()) {
                // bytes contained in audio buffer produced by MediaCodec make up little endian shorts
                // first we recreate short values, then cast them to floats, expected by Oboe resampler
                for (int channel = 0; channel < inputChannelCount; channel++) {
                    int index = (sampleCount - inputFramesLeft) * inputChannelCount + channel;
                    resamplerInputBuffer[channel] = (float) ((short) (((sourceBuffer[index * 2 + 1] & 0xFF) << 8) | sourceBuffer[index * 2] & 0xFF));
                }

                oboeResampler->writeNextFrame(resamplerInputBuffer);
                inputFramesLeft--;
            } else {
                oboeResampler->readNextFrame(resamplerOutputBuffer);
                for (int channel = 0; channel < ouputChannelCount; channel++) {
                    float value = resamplerOutputBuffer[channel];
                    if (value < -32768) {
                        value = -32768;
                    } else if (value > 32767) {
                        value = 32767;
                    }
                    int index = framesProcessed * ouputChannelCount + channel;
                    targetBuffer[index * 2 + 0] = ((short) value) & 0xFF;
                    targetBuffer[index * 2 + 1] = ((short) value >> 8) & 0xFF;
                }
                framesProcessed++;
            }
        }

        return framesProcessed;
    }
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_render_OboeAudioResampler_releaseResampler(
        JNIEnv* env,
        jobject /* this */) {
    if (oboeResampler != nullptr) {
        delete oboeResampler;
        oboeResampler = nullptr;
        inputChannelCount = -1;
        ouputChannelCount = -1;
    }
}