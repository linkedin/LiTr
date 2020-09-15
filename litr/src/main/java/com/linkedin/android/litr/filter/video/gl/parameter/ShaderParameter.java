/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl.parameter;

import android.opengl.GLES20;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base class for all shader attributes
 */
public abstract class ShaderParameter {

    public static final int TYPE_UNIFORM = 0;
    public static final int TYPE_ATTRIBUTE = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ TYPE_UNIFORM, TYPE_ATTRIBUTE})
    @interface Type {}

    @Type public int type;
    @NonNull public String name;

    protected ShaderParameter(@Type int type, @NonNull String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * Apply the parameter to GL program. This is called at each frame render.
     * @param glProgram handle of a GL program containing the parameter
     */
    abstract public void apply(int glProgram);

    protected int getLocation(int glProgram) {
        switch (type) {
            case TYPE_UNIFORM:
                return GLES20.glGetUniformLocation(glProgram, name);
            case TYPE_ATTRIBUTE:
                return GLES20.glGetAttribLocation(glProgram, name);
            default:
                return -1;
        }
    }
}
