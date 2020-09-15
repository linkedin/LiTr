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
 * Three float value shader parameter
 */
public class ShaderParameter3f extends ShaderParameter {

    private float value1;
    private float value2;
    private float value3;

    /**
     * Create shader parameter
     * @param type parameter type (uniform or attribute)
     * @param name parameter name, as defined in shader code
     * @param value1 first parameter value
     * @param value2 second parameter value
     * @param value3 third parameter value
     */
    public ShaderParameter3f(@Type int type, @NonNull String name, float value1, float value2, float value3) {
        super(type, name);

        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
    }

    @Override
    public void apply(int glProgram) {
        GLES20.glUniform3f(getLocation(glProgram), value1, value2, value3);
    }
}
