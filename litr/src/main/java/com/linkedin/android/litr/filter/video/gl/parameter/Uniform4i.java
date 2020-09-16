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
 * Four integer value shader parameter
 */
public class Uniform4i extends ShaderParameter {

    private int value1;
    private int value2;
    private int value3;
    private int value4;

    /**
     * Create shader parameter
     * @param name parameter name, as defined in shader code
     * @param value1 first parameter value
     * @param value2 second parameter value
     * @param value3 third parameter value
     * @param value4 fourth parameter value
     */
    public Uniform4i(@NonNull String name, int value1, int value2, int value3, int value4) {
        super(TYPE_UNIFORM, name);

        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
    }

    @Override
    public void apply(int glProgram) {
        GLES20.glUniform4i(getLocation(glProgram), value1, value2, value3, value4);
    }
}
