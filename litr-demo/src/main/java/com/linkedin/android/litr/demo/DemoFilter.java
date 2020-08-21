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
import com.linkedin.android.litr.filter.video.gl.ExposureFilter;
import com.linkedin.android.litr.filter.video.gl.GammaFilter;
import com.linkedin.android.litr.filter.video.gl.GrayscaleFilter;
import com.linkedin.android.litr.filter.video.gl.HalftoneFilter;
import com.linkedin.android.litr.filter.video.gl.HazeFilter;
import com.linkedin.android.litr.filter.video.gl.OpacityFilter;
import com.linkedin.android.litr.filter.video.gl.ShadowsHighlightsFilter;
import com.linkedin.android.litr.filter.video.gl.HueFilter;
import com.linkedin.android.litr.filter.video.gl.InversionFilter;
import com.linkedin.android.litr.filter.video.gl.PosterizationFilter;
import com.linkedin.android.litr.filter.video.gl.SaturationFilter;
import com.linkedin.android.litr.filter.video.gl.SepiaFilter;
import com.linkedin.android.litr.filter.video.gl.SolarizeFilter;
import com.linkedin.android.litr.filter.video.gl.VibranceFilter;
import com.linkedin.android.litr.filter.video.gl.WhiteBalanceFilter;

enum DemoFilter {
    NONE("No Filter", new DefaultVideoFrameRenderFilter()),
    BRIGHTNESS("Brightness", new BrightnessFilter(0.25f)),
    BULGE_DISTORTION("Bulge Distortion", new BulgeDistortionFilter(new PointF(0.5f, 0.5f), 0.25f, 0.5f)),
    CONTRAST("Contrast", new ContrastFilter(1.7f)),
    CROSS_HATCH("Cross Hatch", new CrossHatchFilter(0.03f, 0.003f)),
    EXPOSURE("Exposure", new ExposureFilter(1.0f)),
    GAMMA("Gamma", new GammaFilter(2.0f)),
    GRAYSCALE("Grayscale", new GrayscaleFilter()),
    HALFTONE("Halftone", new HalftoneFilter(0.01f, 1.0f)),
    HAZE("Haze", new HazeFilter(0.2f, 0.0f)),
    HUE("Hue", new HueFilter(90f)),
    INVERSION("Inversion", new InversionFilter()),
    OPACITY("Opacity", new OpacityFilter(0.7f)),
    POSTERIZATION("Posterization", new PosterizationFilter(10)),
    SATURATION("Saturation", new SaturationFilter(2.0f)),
    SEPIA("Sepia", new SepiaFilter()),
    SOLARIZE("Solarize", new SolarizeFilter(0.5f)),
    SHADOWS_HIGHLIGHTS("Shadows/Highlights", new ShadowsHighlightsFilter(1.0f, 0.0f)),
    VIBRANCE("Vibrance", new VibranceFilter(1.0f)),
    WHITE_BALANCE("White Balance", new WhiteBalanceFilter(3000f, 0.5f));

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
