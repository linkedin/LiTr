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
 * Frame render filter that applies a swirl distortion to video frame
 */
public class SwirlFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform highp vec2 center;\n" +
            "uniform highp float radius;\n" +
            "uniform highp float angle;\n" +

            "void main()\n" +
            "{\n" +
                "highp vec2 textureCoordinateToUse = vTextureCoord;\n" +
                "highp float dist = distance(center, vTextureCoord);\n" +
                "if (dist < radius)\n" +
                "{\n" +
                    "textureCoordinateToUse -= center;\n" +
                    "highp float percent = (radius - dist) / radius;\n" +
                    "highp float theta = percent * percent * angle * 8.0;\n" +
                    "highp float s = sin(theta);\n" +
                    "highp float c = cos(theta);\n" +
                    "textureCoordinateToUse = vec2(dot(textureCoordinateToUse, vec2(c, -s)), dot(textureCoordinateToUse, vec2(s, c)));\n" +
                    "textureCoordinateToUse += center;\n" +
                "}\n" +
                "gl_FragColor = texture2D(sTexture, textureCoordinateToUse );\n" +
            "}";

    /**
     * Create frame render filter
     * @param center center of distortion, in relative coordinates in 0 - 1 range
     * @param radius radius of distortion, in relative coordinaes in 0 - 1 range
     * @param angle angle of distortion
     */
    public SwirlFilter(@NonNull PointF center, float radius, float angle) {
        this(center, radius, angle, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param center center of distortion, in relative coordinates in 0 - 1 range
     * @param radius radius of distortion, in relative coordinaes in 0 - 1 range
     * @param angle angle of distortion
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public SwirlFilter(@NonNull PointF center, float radius, float angle, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform2f("center", center.x, center.y),
                        new Uniform1f("radius", radius),
                        new Uniform1f("angle", angle)
                },
                transform);
    }

}
