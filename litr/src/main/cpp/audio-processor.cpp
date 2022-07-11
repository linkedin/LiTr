/*
 * Copyright 2022 LinkedIn Corporation
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
int outputChannelCount = -1;

float* resamplerInputBuffer = nullptr;
float* resamplerOutputBuffer = nullptr;

void populateInputBuffer(const jbyte *sourceBuffer, int sourceSample, float* inputBuffer, int sourceChannelCount, int targetChannelCount);

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_render_OboeAudioProcessor_initProcessor(
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
    if (sourceChannelCount > 1 && targetChannelCount > 1 && sourceChannelCount != targetChannelCount) {
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, "Multiple channel to multiple channel mixing is not supported");
        }
    }

    inputChannelCount = sourceChannelCount;
    outputChannelCount = targetChannelCount;

    resamplerInputBuffer = new float[outputChannelCount];
    resamplerOutputBuffer = new float[outputChannelCount];
}

extern "C" JNIEXPORT int JNICALL
Java_com_linkedin_android_litr_render_OboeAudioProcessor_processAudioFrame(
        JNIEnv* env,
        jobject,
        jobject jsourceBuffer,
        jint sampleCount,
        jobject jtargetBuffer) {
    if (oboeResampler != nullptr && inputChannelCount > 0 && outputChannelCount > 0) {
        auto sourceBuffer = (jbyte *) env->GetDirectBufferAddress(jsourceBuffer);
        auto targetBuffer = (jbyte *) env->GetDirectBufferAddress(jtargetBuffer);

        int framesProcessed = 0;
        int inputFramesLeft = sampleCount;

        while (inputFramesLeft > 0) {
            if (oboeResampler->isWriteNeeded()) {
                populateInputBuffer(sourceBuffer, sampleCount - inputFramesLeft, resamplerInputBuffer, inputChannelCount, outputChannelCount);
                oboeResampler->writeNextFrame(resamplerInputBuffer);
                inputFramesLeft--;
            } else {
                oboeResampler->readNextFrame(resamplerOutputBuffer);
                for (int channel = 0; channel < outputChannelCount; channel++) {
                    float value = resamplerOutputBuffer[channel];
                    if (value < -32768) {
                        value = -32768;
                    } else if (value > 32767) {
                        value = 32767;
                    }
                    int index = framesProcessed * outputChannelCount + channel;
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
Java_com_linkedin_android_litr_render_OboeAudioProcessor_releaseProcessor(
        JNIEnv* env,
        jobject /* this */) {
    if (oboeResampler != nullptr) {
        delete oboeResampler;
        oboeResampler = nullptr;
        inputChannelCount = -1;
        outputChannelCount = -1;
    }
    if (resamplerInputBuffer != nullptr) {
        delete[] resamplerInputBuffer;
        resamplerInputBuffer = nullptr;
    }
    if (resamplerOutputBuffer != nullptr) {
        delete[] resamplerOutputBuffer;
        resamplerOutputBuffer = nullptr;
    }
}

float getSourceValue(const jbyte *sourceBuffer, int index) {
    // bytes contained in audio buffer produced by MediaCodec make up little endian shorts
    // first we recreate short values, then cast them to floats, expected by Oboe resampler
    return (float) ((short) (((sourceBuffer[index * 2 + 1] & 0xFF) << 8) | sourceBuffer[index * 2] & 0xFF));
}

void populateInputBuffer(const jbyte *sourceBuffer, int sourceSample, float* inputBuffer, int sourceChannelCount, int targetChannelCount) {
    int sourceBufferIndex = sourceSample * sourceChannelCount;
    if (sourceChannelCount == targetChannelCount) {
        // no channel mixing (mono to mono or stereo to stereo), just copy data over
        for (int channel = 0; channel < sourceChannelCount; channel++) {
            inputBuffer[channel] = getSourceValue(sourceBuffer, sourceBufferIndex + channel);
        }
    } else if (sourceChannelCount == 1) {
        // mono to stereo, duplicate source value to both output channel
        for (int channel = 0; channel < targetChannelCount; channel++) {
            inputBuffer[channel] = getSourceValue(sourceBuffer, sourceBufferIndex);
        }
    } else if (targetChannelCount == 1) {
        // stereo to mono, calculate the average source channel values and use it as mono channel value
        float monoValue = 0;
        for (int channel = 0; channel < sourceChannelCount; channel++) {
            monoValue += getSourceValue(sourceBuffer, sourceBufferIndex + channel) / (float) sourceChannelCount;
        }
        inputBuffer[0] = monoValue;
    }
}
