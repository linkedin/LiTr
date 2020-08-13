/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import androidx.annotation.NonNull;

import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter;
import com.linkedin.android.litr.filter.video.gl.GrayscaleFilter;

enum DemoFilter {
    NONE("No Filter", new DefaultVideoFrameRenderFilter()),
    GRAYSCALE("Grayscale", new GrayscaleFilter());

    public String name;
    public GlFilter filter;

    DemoFilter(String name, @NonNull GlFilter filter) {
        this.name = name;
        this.filter = filter;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
