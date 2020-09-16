/*
 * Copyright 2018 Masayuki Suda
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.parameter.ShaderParameter;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform1f;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform2f;

/**
 * Frame render filter that applies a zoom distortion to video frame
 */
public class ZoomBlurFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform highp vec2 blurCenter;\n" +
            "uniform highp float blurSize;\n" +

            "void main()\n" +
            "{\n" +
                "// TODO: Do a more intelligent scaling based on resolution here\n" +
                "highp vec2 samplingOffset = 1.0/100.0 * (blurCenter - vTextureCoord) * blurSize;\n" +
                "lowp vec4 fragmentColor = texture2D(sTexture, vTextureCoord) * 0.18;\n" +
                "fragmentColor += texture2D(sTexture, vTextureCoord + samplingOffset) * 0.15;\n" +
                "fragmentColor += texture2D(sTexture, vTextureCoord + (2.0 * samplingOffset)) *  0.12;\n" +
                "fragmentColor += texture2D(sTexture, vTextureCoord + (3.0 * samplingOffset)) * 0.09;\n" +
                "fragmentColor += texture2D(sTexture, vTextureCoord + (4.0 * samplingOffset)) * 0.05;\n" +
                "fragmentColor += texture2D(sTexture, vTextureCoord - samplingOffset) * 0.15;\n" +
                "fragmentColor += texture2D(sTexture, vTextureCoord - (2.0 * samplingOffset)) *  0.12;\n" +
                "fragmentColor += texture2D(sTexture, vTextureCoord - (3.0 * samplingOffset)) * 0.09;\n" +
                "fragmentColor += texture2D(sTexture, vTextureCoord - (4.0 * samplingOffset)) * 0.05;\n" +
                "gl_FragColor = fragmentColor;\n" +
            "}";

    /**
     * Create frame render filter
     * @param blurCenter center of distortion, in relative coordinates in 0 - 1 range
     * @param blurSize distortion size
     */
    public ZoomBlurFilter(@NonNull PointF blurCenter, float blurSize) {
        this(blurCenter, blurSize, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param blurCenter center of distortion, in relative coordinates in 0 - 1 range
     * @param blurSize distortion size
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public ZoomBlurFilter(@NonNull PointF blurCenter, float blurSize, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform2f("blurCenter", blurCenter.x, blurCenter.y),
                        new Uniform1f("blurSize", blurSize)
                },
                transform);
    }
}
