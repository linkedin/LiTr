/*
 * Copyright 2023 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */

#include <jni.h>
#include <string>

#include "Logging.h"
#include "MediaMuxer.h"

// References:
//  - https://github.com/aosp-mirror/platform_frameworks_base/blob/master/media/java/android/media/MediaMuxer.java
//  - https://sources.debian.org/src/android-framework-23/6.0.1+r72-3/frameworks/base/media/jni/android_media_MediaMuxer.cpp
//  - https://github.com/sztwang/TX2_libstagefright/blob/master/MediaMuxer.cpp

struct fields_t {
    jmethodID arrayID;
};

static fields_t gFields;

/**
 * Helper to throw a JNI based exception.
 *
 * @param env The JNI environment.
 * @param className The name of the Java class which represents the exception.
 * @param message The message associated with the exception.
 */
void jniThrowException(JNIEnv *env, const char* className, const char* message) {
    jclass exClass = env->FindClass(className);
    if (exClass != nullptr) {
        env->ThrowNew(exClass, message);
    }
}

void initByteBuffer(JNIEnv *env) {
    // We only need to look up the ByteBuffer.array() method ID once. It will be stored in a global
    // field, so that it can be re-used after first found, on subsequent sample writes.
    if (gFields.arrayID == nullptr) {
        jclass byteBufClass = env->FindClass("java/nio/ByteBuffer");
        if (byteBufClass == nullptr) {
            LOGE("Unable to find ByteBuffer class");
            jniThrowException(env, "java/lang/IllegalStateException",
                              "Unable to find ByteBuffer class");
        }

        gFields.arrayID = env->GetMethodID(byteBufClass, "array", "()[B");
        if (gFields.arrayID == nullptr) {
            LOGE("Unable to find ByteBuffer array method");
            jniThrowException(env, "java/lang/IllegalStateException",
                              "Unable to find ByteBuffer array method");
        }
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeSetup(
        JNIEnv *env,
        jobject /* this */,
        jstring jOutputPath,
        jstring jFormatName) {
    const char* path = env->GetStringUTFChars(jOutputPath, nullptr);
    const char* formatName = env->GetStringUTFChars(jFormatName, nullptr);

    // Initialise the MediaMuxer, using the path and format provided.
    auto muxer = new MediaMuxer();
    auto err = muxer->init(path, formatName);

    env->ReleaseStringUTFChars(jOutputPath, path);
    env->ReleaseStringUTFChars(jFormatName, formatName);

    if (err == STATUS_ERROR) {
        LOGE("Unable to initialise MediaMuxer");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Failed to initialise the muxer");
    }

    return reinterpret_cast<jlong>(muxer);
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeStart(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject,
        jobjectArray keys,
        jobjectArray values) {
    auto* muxer = reinterpret_cast<MediaMuxer*>(nativeObject);
    if (muxer == nullptr) {
        LOGE("Muxer was not set up correctly");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Muxer was not set up correctly");
    }

    // Check to make sure the given options have the same number of keys as they do values.
    int keysCount = env->GetArrayLength(keys);
    int valuesCount = env->GetArrayLength(values);
    if (keysCount != valuesCount) {
        LOGE("Invalid options specified");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Invalid options specified");
    }

    // The muxer options are provided as two separate arrays that represent the key-value pairs
    // of a dictionary. We have to extract these via JNI and build up our two primative arrays.
    const char* keySet[keysCount];
    const char* valueSet[valuesCount];
    for (int i = 0; i < keysCount; i++) {
        auto jKey = (jstring) env->GetObjectArrayElement(keys, i);
        const char* key = env->GetStringUTFChars(jKey, nullptr);
        keySet[i] = key;

        auto jValue = (jstring) env->GetObjectArrayElement(values, i);
        const char* value = env->GetStringUTFChars(jValue, nullptr);
        valueSet[i] = value;
    }

    // Start the muxer with the given options.
    auto err = muxer->start(keySet, valueSet, keysCount);

    // Ensure that all UTF chars are released.
    for (int i = 0; i < keysCount; i++) {
        auto jKey = (jstring) env->GetObjectArrayElement(keys, i);
        env->ReleaseStringUTFChars(jKey, keySet[i]);

        auto jValue = (jstring) env->GetObjectArrayElement(values, i);
        env->ReleaseStringUTFChars(jValue, valueSet[i]);
    }

    if (err == STATUS_ERROR) {
        LOGE("Unable to start MediaMuxer");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Failed to start the muxer");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeStop(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject) {
    auto* muxer = reinterpret_cast<MediaMuxer*>(nativeObject);
    if (muxer == nullptr) {
        LOGE("Muxer was not set up correctly");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Muxer was not set up correctly");
    }

    auto err = muxer->stop();
    if (err == STATUS_ERROR) {
        LOGE("Unable to stop MediaMuxer");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Failed to stop the muxer");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeRelease(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject) {
    auto* muxer = reinterpret_cast<MediaMuxer*>(nativeObject);
    delete muxer;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeAddAudioTrack(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject,
        jstring codecId,
        jint bitrate,
        jint channelCount,
        jint sampleRate,
        jint frameSize,
        jobject byteBuf,
        jint size) {
    auto* muxer = reinterpret_cast<MediaMuxer*>(nativeObject);
    if (muxer == nullptr) {
        LOGE("Muxer was not set up correctly");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Muxer was not set up correctly");
    }

    initByteBuffer(env);
    if (gFields.arrayID == nullptr) {
        LOGE("Unable to find ByteBuffer array method");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Unable to find ByteBuffer array method");
    }

    const char* codecName = env->GetStringUTFChars(codecId, nullptr);

    auto byteArray = (jbyteArray)env->CallObjectMethod(byteBuf, gFields.arrayID);
    if (byteArray == nullptr) {
        LOGE("byteArray is null");
        jniThrowException(env, "java/lang/IllegalArgumentException",
                          "byteArray is null");
        return -1;
    }

    auto dst = env->GetByteArrayElements(byteArray, nullptr);
    auto dstSize = env->GetArrayLength(byteArray);

    auto streamIndex = muxer->addAudioStream(
            codecName,
            bitrate,
            channelCount,
            sampleRate,
            frameSize,
            (uint8_t *) dst,
            size);

    env->ReleaseStringUTFChars(codecId, codecName);
    env->ReleaseByteArrayElements(byteArray, (jbyte *)dst, 0);

    if (streamIndex < 0) {
        LOGE("Unable to add video track");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Unable to add video track");
    }

    return streamIndex;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeAddVideoTrack(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject,
        jstring codecId,
        jint bitrate,
        jint width,
        jint height,
        jobject byteBuf,
        jint size) {
    auto* muxer = reinterpret_cast<MediaMuxer*>(nativeObject);
    if (muxer == nullptr) {
        LOGE("Muxer was not set up correctly");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Muxer was not set up correctly");
    }

    initByteBuffer(env);
    if (gFields.arrayID == nullptr) {
        LOGE("Unable to find ByteBuffer array method");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Unable to find ByteBuffer array method");
    }

    const char* codecName = env->GetStringUTFChars(codecId, nullptr);

    auto byteArray = (jbyteArray)env->CallObjectMethod(byteBuf, gFields.arrayID);
    if (byteArray == nullptr) {
        LOGE("byteArray is null");
        jniThrowException(env, "java/lang/IllegalArgumentException",
                          "byteArray is null");
        return -1;
    }

    auto dst = env->GetByteArrayElements(byteArray, nullptr);
    auto dstSize = env->GetArrayLength(byteArray);

    auto streamIndex = muxer->addVideoStream(
            codecName,
            bitrate,
            width,
            height,
            (uint8_t *) dst,
            size);

    env->ReleaseStringUTFChars(codecId, codecName);
    env->ReleaseByteArrayElements(byteArray, (jbyte *)dst, 0);

    if (streamIndex < 0) {
        LOGE("Unable to add video track");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Unable to add video track");
    }

    return streamIndex;
}

extern "C" JNIEXPORT void JNICALL
Java_com_linkedin_android_litr_muxers_NativeMediaMuxer_nativeWriteSampleData(
        JNIEnv *env,
        jobject /* this */,
        jlong nativeObject,
        jint trackIndex,
        jobject byteBuf,
        jint offset,
        jint size,
        jlong presentationTimeUs,
        jint flags) {
    auto* muxer = reinterpret_cast<MediaMuxer*>(nativeObject);
    if (muxer == nullptr) {
        LOGE("Muxer was not set up correctly");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Muxer was not set up correctly");
    }

    initByteBuffer(env);
    if (gFields.arrayID == nullptr) {
        LOGE("Unable to find ByteBuffer array method");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "Unable to find ByteBuffer array method");
    }

    auto byteArray = (jbyteArray)env->CallObjectMethod(byteBuf, gFields.arrayID);
    if (byteArray == nullptr) {
        LOGE("byteArray is null");
        jniThrowException(env, "java/lang/IllegalArgumentException",
                          "byteArray is null");
        return;
    }

    auto dst = env->GetByteArrayElements(byteArray, nullptr);
    auto dstSize = env->GetArrayLength(byteArray);

    if (dstSize < (offset + size)) {
        LOGE("writeSampleData saw wrong dstSize %lld, size  %d, offset %d", (long long)dstSize, size, offset);
        env->ReleaseByteArrayElements(byteArray, (jbyte *)dst, 0);
        jniThrowException(env, "java/lang/IllegalArgumentException",
                          "sample has a wrong size");
        return;
    }

    // Now that we have access to the underlying buffer, let's use that to build a suitable sample
    // to write via the Muxer.
    auto err = muxer->writeSampleData(trackIndex, (uint8_t *) dst + offset, size, presentationTimeUs, flags);

    env->ReleaseByteArrayElements(byteArray, (jbyte *)dst, 0);

    if (err == STATUS_ERROR) {
        LOGE("writeSampleData returned an error");
        jniThrowException(env, "java/lang/IllegalStateException",
                          "writeSampleData returned an error");
    }
}