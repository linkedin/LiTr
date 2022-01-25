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
            MultiChannelResampler::Quality::High);
    channel_count = channelCount;
}

extern "C" JNIEXPORT int JNICALL
Java_com_linkedin_android_litr_render_AudioRenderer_resample(
        JNIEnv* env,
        jobject,
        jobject jsourceBuffer,
        jint sampleCount,
        jobject jtargetBuffer) {
    if (audio_resampler != nullptr && channel_count > 0) {
        auto sourceBuffer = (jbyte *) env->GetDirectBufferAddress(jsourceBuffer);
        auto targetBuffer = (jbyte *) env->GetDirectBufferAddress(jtargetBuffer);

        auto inputBuffer = new float[sampleCount * channel_count];
        auto outputBuffer = new float[channel_count];

        // bytes contained in audio buffer produced by MediaCodec contains make up little endian shorts
        // first we recreate short values, then simply cast them to floats, expected by Oboe resampler
        for (int index = 0; index < sampleCount * channel_count; index++) {
            inputBuffer[index] = (float) ((short) (((sourceBuffer[index * 2 + 1] & 0xFF) << 8) | sourceBuffer[index * 2] & 0xFF));
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
                    float value = outputBuffer[channel];
                    if (value < -32768) {
                        value = -32768;
                    } else if (value > 32767) {
                        value = 32767;
                    }
                    int index = framesProcessed * channel_count + channel;
                    targetBuffer[index * 2 + 0] = ((short) value) & 0xFF;
                    targetBuffer[index * 2 + 1] = ((short) value >> 8) & 0xFF;
                }
                framesProcessed++;
            }
        }

//        int factor = 2;
//        int framesProcessed = sampleCount / factor;
//        for (int sample = 0; sample < framesProcessed; sample++) {
//            for (int channel = 0; channel < channel_count; channel++) {
//                targetBuffer[(sample * channel_count + channel) * 2 + 0] = sourceBuffer[(factor * sample * channel_count + channel) * 2 + 0];
//                targetBuffer[(sample * channel_count + channel) * 2 + 1] = sourceBuffer[(factor * sample * channel_count + channel) * 2 + 1];
//            }
//        }

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