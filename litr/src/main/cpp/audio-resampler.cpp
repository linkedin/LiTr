#include <jni.h>
#include "oboe_resampler/MultiChannelResampler.h"

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_render_AudioRenderer_initAudioResampler(
        JNIEnv* env,
        jobject /* this */,
        jint channelCount,
        jint sourceSampleRate,
        jint targetSampleRate) {
    // TODO implement
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_linkedin_android_litr_render_AudioRenderer_isWriteNeeded(
        JNIEnv* env,
        jobject /* this */) {
    // TODO implement
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_render_AudioRenderer_writeNextAudioFrame(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray frame) {
    // TODO implement
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_linkedin_android_litr_render_AudioRenderer_readNextAudioFrame(
        JNIEnv* env,
        jobject /* this */) {
    // TODO implement
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_render_AudioRenderer_releaseAudioResampler(
        JNIEnv* env,
        jobject /* this */) {
    // TODO implement
}