#include <jni.h>
#include <android/log.h>
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
        jshort* sourceBuffer = env->GetShortArrayElements(jsourceBuffer, nullptr);
        jshort* targetBuffer = env->GetShortArrayElements(jtargetBuffer, nullptr);

        auto inputBuffer = new float[sourceBufferSize];
        auto outputBuffer = new float[targetBufferSize];

        float maxSource = -1;
        for (int index = 0; index < sourceBufferSize; index++) {
            inputBuffer[index] = (float) sourceBuffer[index];
            if (maxSource < inputBuffer[index]) {
                maxSource = inputBuffer[index];
            }
        }

        int sourceBufferPos = 0;
        int targetBufferPos = 0;

        int framesProcessed = 0;
        int inputFramesLeft = sourceBufferSize / channel_count;

        while (inputFramesLeft > 0) {
            if (audio_resampler->isWriteNeeded()) {
//                for (int channel = 0; channel < channel_count; channel++) {
//                    inputBuffer[channel] = (float) sourceBuffer[sourceBufferPos + channel];
//                }
                audio_resampler->writeNextFrame(inputBuffer);
                inputBuffer += channel_count;
                inputFramesLeft--;
            } else {
                audio_resampler->readNextFrame(outputBuffer);
//                for (int channel = 0; channel < channel_count; channel++) {
//                    targetBuffer[targetBufferPos + channel] = (short) outputBuffer[channel];
//                }
                outputBuffer += channel_count;
                framesProcessed++;
            }
        }

        float maxOutput = -1;
        for (int index = 0; index < framesProcessed; index++) {
            for (int channel = 0; channel < channel_count; channel++) {
                if (maxOutput < outputBuffer[index * channel_count + channel]) {
                    maxOutput = outputBuffer[index * channel_count + channel];
                }
                targetBuffer[index * channel_count + channel] = (short) (outputBuffer[index * channel_count + channel]);
            }
        }

        char* log = new char[100];
        snprintf(log, 100, "Max source %f, max output %f", maxSource, maxOutput);
        __android_log_write(ANDROID_LOG_DEBUG, "AudioRenderer", log);

//        int factor = 2;
//        int framesProcessed = sourceBufferSize / (factor * channel_count);
//        for (int i = 0; i < framesProcessed; i++) {
//            for (int c = 0; c < channel_count; c++) {
//                targetBuffer[i * channel_count + c] = sourceBuffer[i * factor * channel_count + c];
//            }
//        }

        env->ReleaseShortArrayElements(jsourceBuffer, sourceBuffer, 0);
        env->ReleaseShortArrayElements(jtargetBuffer, targetBuffer, 0);

//        delete[] inputBuffer;
//        delete[] outputBuffer;

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