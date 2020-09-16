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
 * Four float value shader parameter
 */
public class Uniform4f extends ShaderParameter {

    private float value1;
    private float value2;
    private float value3;
    private float value4;

    /**
     * Create shader parameter
     * @param name parameter name, as defined in shader code
     * @param value1 first parameter value
     * @param value2 second parameter value
     * @param value3 third parameter value
     * @param value4 fourth parameter value
     */
    public Uniform4f(@NonNull String name, float value1, float value2, float value3, float value4) {
        super(TYPE_UNIFORM, name);

        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
    }

    @Override
    public void apply(int glProgram) {
        GLES20.glUniform4f(getLocation(glProgram), value1, value2, value3, value4);
    }
}
