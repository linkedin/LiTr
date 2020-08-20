/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.graphics.PointF;

import androidx.annotation.NonNull;

import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.filter.video.gl.BrightnessFilter;
import com.linkedin.android.litr.filter.video.gl.BulgeDistortionFilter;
import com.linkedin.android.litr.filter.video.gl.ContrastFilter;
import com.linkedin.android.litr.filter.video.gl.CrossHatchFilter;
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter;
import com.linkedin.android.litr.filter.video.gl.GrayscaleFilter;

enum DemoFilter {
    NONE("No Filter", new DefaultVideoFrameRenderFilter()),
    BRIGHTNESS("Brightness", new BrightnessFilter(0.25f)),
    CONTRAST("Contrast", new ContrastFilter(1.7f)),
    CROSS_HATCH("Cross Hatch", new CrossHatchFilter(0.03f, 0.003f)),
    GRAYSCALE("Grayscale", new GrayscaleFilter()),
    BULGE_DISTORTION("Bulge Distortion", new BulgeDistortionFilter(new PointF(0.5f, 0.5f), 0.25f, 0.5f));

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
