/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl.parameter;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import java.nio.IntBuffer;

/**
 * One integer element vector shader parameter
 */
public class Uniform1iv extends ShaderParameter {

    private int count;
    private IntBuffer buffer;

    /**
     * Create shader parameter
     * @param name parameter name, as defined in shader code
     * @param count number of vectors
     * @param buffer buffer containing new vector values
     */
    public Uniform1iv(@NonNull String name, int count, @NonNull int[] buffer) {
        this(name, count, IntBuffer.wrap(buffer));
    }

    /**
     * Create shader parameter
     * @param name parameter name, as defined in shader code
     * @param count number of vectors
     * @param buffer buffer containing new vector values
     */
    public Uniform1iv(@NonNull String name, int count, @NonNull IntBuffer buffer) {
        super(TYPE_UNIFORM, name);

        this.count = count;
        this.buffer = buffer;
    }

    @Override
    public void apply(int glProgram) {
        GLES20.glUniform1iv(getLocation(glProgram), count, buffer);
    }
}
