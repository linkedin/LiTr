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

/**
 * 4x4 float value matrix shader parameter
 */
public class ShaderParameterMatrix4fv extends ShaderParameter {

    private int count;
    private boolean transpose;
    private float[] matrix;
    private int offset;

    /**
     * Create shader parameter
     * @param type parameter type (uniform or attribute)
     * @param name parameter name, as defined in shader code
     * @param count number of matrices
     * @param transpose flag indicating if matrix is transposed
     * @param matrix matrix values
     * @param offset matrix offset
     */
    public ShaderParameterMatrix4fv(@Type int type, @NonNull String name, int count, boolean transpose, float[] matrix, int offset) {
        super(type, name);

        this.count = count;
        this.transpose = transpose;
        this.matrix = matrix;
        this.offset = offset;
    }

    @Override
    public void apply(int glProgram) {
        GLES20.glUniformMatrix4fv(getLocation(glProgram), count, transpose, matrix, offset);
    }
}
