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
 * One float value shader parameter
 */
public class ShaderParameter1f extends ShaderParameter {

    private float value;

    /**
     * Create shader parameter
     * @param type parameter type (uniform or attribute)
     * @param name parameter name, as defined in shader code
     * @param value parameter value
     */
    public ShaderParameter1f(@Type int type, @NonNull String name, float value) {
        super(type, name);

        this.value = value;
    }

    @Override
    public void apply(int glProgram) {
        GLES20.glUniform1f(getLocation(glProgram), value);
    }
}
