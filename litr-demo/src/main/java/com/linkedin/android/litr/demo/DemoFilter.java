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
import com.linkedin.android.litr.filter.video.gl.ColorBalanceFilter;
import com.linkedin.android.litr.filter.video.gl.ColorMatrixFilter;
import com.linkedin.android.litr.filter.video.gl.ColorMonochromeFilter;
import com.linkedin.android.litr.filter.video.gl.ContrastFilter;
import com.linkedin.android.litr.filter.video.gl.CrossHatchFilter;
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter;
import com.linkedin.android.litr.filter.video.gl.ExposureFilter;
import com.linkedin.android.litr.filter.video.gl.FalseColorFilter;
import com.linkedin.android.litr.filter.video.gl.GammaFilter;
import com.linkedin.android.litr.filter.video.gl.GaussianBlurFilter;
import com.linkedin.android.litr.filter.video.gl.GlassSphereFilter;
import com.linkedin.android.litr.filter.video.gl.GrayscaleFilter;
import com.linkedin.android.litr.filter.video.gl.HalftoneFilter;
import com.linkedin.android.litr.filter.video.gl.HazeFilter;
import com.linkedin.android.litr.filter.video.gl.HueFilter;
import com.linkedin.android.litr.filter.video.gl.InversionFilter;
import com.linkedin.android.litr.filter.video.gl.KuwaharaFilter;
import com.linkedin.android.litr.filter.video.gl.LaplacianFilter;
import com.linkedin.android.litr.filter.video.gl.LevelsFilter;
import com.linkedin.android.litr.filter.video.gl.LocalBinaryPatternFilter;
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
import com.linkedin.android.litr.filter.video.gl.VignetteFilter;
import com.linkedin.android.litr.filter.video.gl.WeakPixelInclusionFilter;
import com.linkedin.android.litr.filter.video.gl.WhiteBalanceFilter;
import com.linkedin.android.litr.filter.video.gl.ZoomBlurFilter;

enum DemoFilter {
    NONE("No Filter", new DefaultVideoFrameRenderFilter()),
    BILATERAL("Bilateral", new BilateralFilter(0.004f, 0.004f, 1.0f)),
    BRIGHTNESS("Brightness", new BrightnessFilter(0.25f)),
    BULGE_DISTORTION("Bulge Distortion", new BulgeDistortionFilter(new PointF(0.5f, 0.5f), 0.25f, 0.5f)),
    CONTRAST("Contrast", new ContrastFilter(1.7f)),
    COLOR_BALANCE("Color Balance", new ColorBalanceFilter(new float[]{0.0f, 0.0f, 1.0f}, new float[]{0.0f, 0.5f, 0.0f}, new float[]{0.3f, 0.0f, 0.0f}, true)),
    COLOR_MATRIX("Color Matrix", new ColorMatrixFilter(new float[]{
            0.67f, 0f, 0f, 0f,
            0.0f, 0.53f, 0f, 0f,
            0f, 0f, 0.78f, 0f,
            0f, 0f, 0f, 0f, 1.0f
    }, 1.0f)),
    COLOR_MONOCHROME("Color Monochrome", new ColorMonochromeFilter(new float[]{0.6f, 0.45f, 0.3f}, 1.0f)),
    CGA_COLORSPACE("CGA Colorspace", new CgaColorspaceFilter()),
    CROSS_HATCH("Cross Hatch", new CrossHatchFilter(0.03f, 0.003f)),
    EXPOSURE("Exposure", new ExposureFilter(1.0f)),
    FALSE_COLOR("False Color", new FalseColorFilter(new float[]{0.0f, 0.0f, 0.5f}, new float[]{1.0f, 0.0f, 0.0f})),
    GAMMA("Gamma", new GammaFilter(2.0f)),
    GAUSSIAN_BLUR("Gaussian Blur", new GaussianBlurFilter(0.01f, 0.01f, 0.2f)),
    GLASS_SPHERE("Glass Sphere", new GlassSphereFilter(new PointF(0.5f, 0.5f), 0.35f, 1.0f, 0.71f)),
    GRAYSCALE("Grayscale", new GrayscaleFilter()),
    HALFTONE("Halftone", new HalftoneFilter(0.01f, 1.0f)),
    HAZE("Haze", new HazeFilter(0.2f, 0.0f)),
    HUE("Hue", new HueFilter(90f)),
    INVERSION("Inversion", new InversionFilter()),
    KUWAHARA("Kuwahara", new KuwaharaFilter(6)),
    LAPLACIAN("Laplacian", new LaplacianFilter(new float[]{
            0.5f, 1.0f, 0.5f,
            1.0f, -6.0f, 1.0f,
            0.5f, 1.0f, 0.5f
    }, 0.01f, 0.01f)),
    LEVELS("Levels", new LevelsFilter(new float[]{0.0f, 0.0f, 0.25f}, new float[]{1.0f, 1.0f, 0.6f}, new float[]{1.0f, 1.0f, 1.0f}, new float[]{0.0f, 0.0f, 0.0f}, new float[]{1.0f, 1.0f, 1.0f})),
    LOCAL_BINARY_PATTERN("Local Binary Pattern", new LocalBinaryPatternFilter(0.002f,0.002f)),
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
    VIGNETTE("Vignette", new VignetteFilter(new PointF(0.5f, 0.5f), new float[]{0.0f, 0.0f, 1.0f}, 0.3f, 0.75f)),
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
