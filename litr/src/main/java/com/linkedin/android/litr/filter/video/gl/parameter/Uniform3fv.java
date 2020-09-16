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

import java.nio.FloatBuffer;

/**
 * Three element vector shader parameter
 */
public class Uniform3fv extends ShaderParameter {

    private int count;
    private FloatBuffer buffer;

    /**
     * Create shader parameter
     * @param name parameter name, as defined in shader code
     * @param count number of vectors
     * @param buffer buffer containing vector values
     */
    public Uniform3fv(@NonNull String name, int count, FloatBuffer buffer) {
        super(TYPE_UNIFORM, name);

        this.count = count;
        this.buffer = buffer;
    }

    @Override
    public void apply(int glProgram) {
        GLES20.glUniform3fv(getLocation(glProgram), count, buffer);
    }
}
