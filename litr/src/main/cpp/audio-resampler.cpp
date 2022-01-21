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
        jshortArray jsourceBuffer,
        jint sourceBufferSize,
        jshortArray jtargetBuffer,
        jint targetBufferSize) {
    if (audio_resampler != nullptr && channel_count > 0) {
        jshort* sourceBuffer = env->GetShortArrayElements(jsourceBuffer, JNI_FALSE);
        jshort* targetBuffer = env->GetShortArrayElements(jtargetBuffer, JNI_FALSE);

        auto inputBuffer = new float[sourceBufferSize];
        auto outputBuffer = new float[channel_count];

        for (int index = 0; index < sourceBufferSize; index++) {
            inputBuffer[index] = ((float) sourceBuffer[index]);
        }

        int framesProcessed = 0;
        int inputFramesLeft = sourceBufferSize / channel_count;

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

        env->ReleaseShortArrayElements(jsourceBuffer, sourceBuffer, 0);
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