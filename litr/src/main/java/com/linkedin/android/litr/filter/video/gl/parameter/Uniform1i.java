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
 * One integer value shader parameter
 */
public class Uniform1i extends ShaderParameter {

    private int value;

    /**
     * Create shader parameter
     * @param name parameter name, as defined in shader code
     * @param value parameter value
     */
    public Uniform1i(@NonNull String name, int value) {
        super(TYPE_UNIFORM, name);

        this.value = value;
    }

    @Override
    public void apply(int glProgram) {
        GLES20.glUniform1i(getLocation(glProgram), value);
    }
}
