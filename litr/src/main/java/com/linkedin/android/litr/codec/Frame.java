/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.codec;

import android.media.MediaCodec;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

/**
 * A container class for frame data and metadata
 */
public class Frame {
    public final int tag;
    @Nullable public final ByteBuffer buffer;
    @NonNull public final MediaCodec.BufferInfo bufferInfo;

    public Frame(int tag, @Nullable ByteBuffer buffer, @Nullable MediaCodec.BufferInfo bufferInfo) {
        this.tag = tag;
        this.buffer = buffer;

        if (bufferInfo == null) {
            this.bufferInfo = new MediaCodec.BufferInfo();
        } else {
            this.bufferInfo = bufferInfo;
        }
    }
}
