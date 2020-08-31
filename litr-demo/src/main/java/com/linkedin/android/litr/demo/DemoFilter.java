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
import com.linkedin.android.litr.filter.video.gl.BilateralFilter;
import com.linkedin.android.litr.filter.video.gl.BrightnessFilter;
import com.linkedin.android.litr.filter.video.gl.BulgeDistortionFilter;
import com.linkedin.android.litr.filter.video.gl.CgaColorspaceFilter;
import com.linkedin.android.litr.filter.video.gl.ContrastFilter;
import com.linkedin.android.litr.filter.video.gl.CrossHatchFilter;
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter;
import com.linkedin.android.litr.filter.video.gl.ExposureFilter;
import com.linkedin.android.litr.filter.video.gl.GammaFilter;
import com.linkedin.android.litr.filter.video.gl.GaussianBlurFilter;
import com.linkedin.android.litr.filter.video.gl.GrayscaleFilter;
import com.linkedin.android.litr.filter.video.gl.HalftoneFilter;
import com.linkedin.android.litr.filter.video.gl.HazeFilter;
import com.linkedin.android.litr.filter.video.gl.HueFilter;
import com.linkedin.android.litr.filter.video.gl.InversionFilter;
import com.linkedin.android.litr.filter.video.gl.OpacityFilter;
import com.linkedin.android.litr.filter.video.gl.PixelationFilter;
import com.linkedin.android.litr.filter.video.gl.PosterizationFilter;
import com.linkedin.android.litr.filter.video.gl.RgbFilter;
import com.linkedin.android.litr.filter.video.gl.SaturationFilter;
import com.linkedin.android.litr.filter.video.gl.SepiaFilter;
import com.linkedin.android.litr.filter.video.gl.ShadowsHighlightsFilter;
import com.linkedin.android.litr.filter.video.gl.SharpenFilter;
import com.linkedin.android.litr.filter.video.gl.SolarizeFilter;
import com.linkedin.android.litr.filter.video.gl.SphereRefractionFilter;
import com.linkedin.android.litr.filter.video.gl.SwirlFilter;
import com.linkedin.android.litr.filter.video.gl.ToonFilter;
import com.linkedin.android.litr.filter.video.gl.VibranceFilter;
import com.linkedin.android.litr.filter.video.gl.WeakPixelInclusionFilter;
import com.linkedin.android.litr.filter.video.gl.WhiteBalanceFilter;
import com.linkedin.android.litr.filter.video.gl.ZoomBlurFilter;

enum DemoFilter {
    NONE("No Filter", new DefaultVideoFrameRenderFilter()),
    BILATERAL("Bilateral", new BilateralFilter(0.004f, 0.004f, 1.0f)),
    BRIGHTNESS("Brightness", new BrightnessFilter(0.25f)),
    BULGE_DISTORTION("Bulge Distortion", new BulgeDistortionFilter(new PointF(0.5f, 0.5f), 0.25f, 0.5f)),
    CONTRAST("Contrast", new ContrastFilter(1.7f)),
    CGA_COLORSPACE("CGA Colorspace", new CgaColorspaceFilter()),
    CROSS_HATCH("Cross Hatch", new CrossHatchFilter(0.03f, 0.003f)),
    EXPOSURE("Exposure", new ExposureFilter(1.0f)),
    GAMMA("Gamma", new GammaFilter(2.0f)),
    GAUSSIAN_BLUR("Gaussian Blur", new GaussianBlurFilter(0.01f, 0.01f, 0.2f)),
    GRAYSCALE("Grayscale", new GrayscaleFilter()),
    HALFTONE("Halftone", new HalftoneFilter(0.01f, 1.0f)),
    HAZE("Haze", new HazeFilter(0.2f, 0.0f)),
    HUE("Hue", new HueFilter(90f)),
    INVERSION("Inversion", new InversionFilter()),
    OPACITY("Opacity", new OpacityFilter(0.7f)),
    PIXELATION("Pixelation", new PixelationFilter(0.01f, 0.01f, 1.0f)),
    POSTERIZATION("Posterization", new PosterizationFilter(10)),
    RGB("RGB Adjustment", new RgbFilter(1.5f, 1.0f, 10.5f)),
    SATURATION("Saturation", new SaturationFilter(2.0f)),
    SEPIA("Sepia", new SepiaFilter()),
    SHADOWS_HIGHLIGHTS("Shadows/Highlights", new ShadowsHighlightsFilter(1.0f, 0.0f)),
    SHARPEN("Sharpen", new SharpenFilter(0.004f, 0.004f, 1.0f)),
    SOLARIZE("Solarize", new SolarizeFilter(0.5f)),
    SPHERE_REFRACTION("Sphere Refraction", new SphereRefractionFilter(new PointF(0.5f, 0.5f), 0.5f, 1.0f, 0.71f)),
    SWIRL("Swirl", new SwirlFilter(new PointF(0.5f, 0.5f), 0.5f, 1.0f)),
    TOON("Toon", new ToonFilter(0.01f, 0.01f, 0.2f, 10f)),
    VIBRANCE("Vibrance", new VibranceFilter(1.0f)),
    WEAK_PIXEL_INCLUSION("Weak Pixel Inclusion", new WeakPixelInclusionFilter(0.01f, 0.01f)),
    WHITE_BALANCE("White Balance", new WhiteBalanceFilter(3000f, 0.5f)),
    ZOOM_BLUR("Zoom Blur", new ZoomBlurFilter(new PointF(0.5f, 0.5f), 1.0f));

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
