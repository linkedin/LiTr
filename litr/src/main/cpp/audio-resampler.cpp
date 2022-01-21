#include <jni.h>
#include "oboe_resampler/MultiChannelResampler.h"

using namespace resampler;

MultiChannelResampler* audio_resampler = nullptr;
int channel_count = -1;

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_render_AudioRenderer_initAudioResampler(
        JNIEnv* env,
        jobject /* this */,
        jint channelCount,
        jint sourceSampleRate,
        jint targetSampleRate) {
    audio_resampler = MultiChannelResampler::make(
            channelCount,
            sourceSampleRate,
            targetSampleRate,
            MultiChannelResampler::Quality::Fastest);
    channel_count = channelCount;
}

extern "C" JNIEXPORT int JNICALL
Java_com_linkedin_android_litr_render_AudioRenderer_resample(
        JNIEnv* env,
        jobject,
        jobject jsourceBuffer,
        jint sampleCount,
        jshortArray jtargetBuffer) {
    if (audio_resampler != nullptr && channel_count > 0) {
        auto sourceBuffer = (jbyte *) env->GetDirectBufferAddress(jsourceBuffer);
        jshort* targetBuffer = env->GetShortArrayElements(jtargetBuffer, JNI_FALSE);

        auto inputBuffer = new float[sampleCount * channel_count];
        auto outputBuffer = new float[channel_count];

        for (int index = 0; index < sampleCount * channel_count; index++) {
            short value = (sourceBuffer[index * 2] << 8) | sourceBuffer[index * 2 + 1];
            inputBuffer[index] = ((float) value);
        }

        int framesProcessed = 0;
        int inputFramesLeft = sampleCount;

        while (inputFramesLeft > 0) {
            if (audio_resampler->isWriteNeeded()) {
                audio_resampler->writeNextFrame(inputBuffer);
                inputBuffer += channel_count;
                inputFramesLeft--;
            } else {
                audio_resampler->readNextFrame(outputBuffer);
                for (int channel = 0; channel < channel_count; channel++) {
                    targetBuffer[framesProcessed * channel_count + channel] = (short) *(outputBuffer + channel);
                }
                framesProcessed++;
            }
        }

        env->ReleaseShortArrayElements(jtargetBuffer, targetBuffer, 0);

        return framesProcessed;
    }
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_render_AudioRenderer_releaseAudioResampler(
        JNIEnv* env,
        jobject /* this */) {
    if (audio_resampler != nullptr) {
        delete audio_resampler;
        audio_resampler = nullptr;
        channel_count = -1;
    }
}