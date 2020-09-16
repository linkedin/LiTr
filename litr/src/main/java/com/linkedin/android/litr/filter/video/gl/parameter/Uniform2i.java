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
 * Two integer value shader parameter
 */
public class Uniform2i extends ShaderParameter {

    private int value1;
    private int value2;

    /**
     * Create shader parameter
     * @param name parameter name, as defined in shader code
     * @param value1 first parameter value
     * @param value2 second parameter value
     */
    public Uniform2i(@NonNull String name, int value1, int value2) {
        super(TYPE_UNIFORM, name);

        this.value1 = value1;
        this.value2 = value2;
    }

    @Override
    public void apply(int glProgram) {
        GLES20.glUniform2i(getLocation(glProgram), value1, value2);
    }
}
