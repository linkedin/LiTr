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
 * 2x2 float value matrix shader parameter
 */
public class UniformMatrix2fv extends ShaderParameter {

    private int count;
    private boolean transpose;
    private float[] matrix;
    private int offset;

    private FloatBuffer buffer;

    /**
     * Create shader parameter
     * @param name parameter name, as defined in shader code
     * @param count number of matrices
     * @param transpose flag indicating if matrix is transposed
     * @param matrix matrix values
     * @param offset matrix offset
     */
    public UniformMatrix2fv(@NonNull String name, int count, boolean transpose, @NonNull float[] matrix, int offset) {
        super(TYPE_UNIFORM, name);

        this.count = count;
        this.transpose = transpose;
        this.matrix = matrix;
        this.offset = offset;
    }

    /**
     * Create shader parameter
     * @param name parameter name, as defined in shader code
     * @param count number of matrices
     * @param transpose flag indicating if matrix is transposed
     * @param buffer buffer containing matrix values
     */
    public UniformMatrix2fv(@NonNull String name, int count, boolean transpose, @NonNull FloatBuffer buffer) {
        super(TYPE_UNIFORM, name);

        this.count = count;
        this.transpose = transpose;
        this.buffer = buffer;
    }

    @Override
    public void apply(int glProgram) {
        if (buffer != null) {
            GLES20.glUniformMatrix2fv(getLocation(glProgram), count, transpose, buffer);
        } else {
            GLES20.glUniformMatrix2fv(getLocation(glProgram), count, transpose, matrix, offset);
        }
    }
}
