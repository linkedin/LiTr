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

import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.parameter.ShaderParameter;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform1f;

/**
 * Frame render filter that adjusts the hue of video pixels
 */
public class HueFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision highp float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform mediump float hueAdjustment;\n" +
            "const highp vec4 kRGBToYPrime = vec4 (0.299, 0.587, 0.114, 0.0);\n" +
            "const highp vec4 kRGBToI = vec4 (0.595716, -0.274453, -0.321263, 0.0);\n" +
            "const highp vec4 kRGBToQ = vec4 (0.211456, -0.522591, 0.31135, 0.0);\n" +
            "const highp vec4 kYIQToR = vec4 (1.0, 0.9563, 0.6210, 0.0);\n" +
            "const highp vec4 kYIQToG = vec4 (1.0, -0.2721, -0.6474, 0.0);\n" +
            "const highp vec4 kYIQToB = vec4 (1.0, -1.1070, 1.7046, 0.0);\n" +

            "void main ()\n" +
                "{\n" +
                "// Sample the input pixel\n" +
                "highp vec4 color = texture2D(sTexture, vTextureCoord);\n" +

                "// Convert to YIQ\n" +
                "highp float YPrime = dot (color, kRGBToYPrime);\n" +
                "highp float I = dot (color, kRGBToI);\n" +
                "highp float Q = dot (color, kRGBToQ);\n" +

                "// Calculate the hue and chroma\n" +
                "highp float hue = atan (Q, I);\n" +
                "highp float chroma = sqrt (I * I + Q * Q);\n" +

                "// Make the user's adjustments\n" +
                "hue += (-hueAdjustment); //why negative rotation?\n" +

                "// Convert back to YIQ\n" +
                "Q = chroma * sin (hue);\n" +
                "I = chroma * cos (hue);\n" +

                "// Convert back to RGB\n" +
                "highp vec4 yIQ = vec4 (YPrime, I, Q, 0.0);\n" +
                "color.r = dot (yIQ, kYIQToR);\n" +
                "color.g = dot (yIQ, kYIQToG);\n" +
                "color.b = dot (yIQ, kYIQToB);\n" +

                "// Save the result\n" +
                "gl_FragColor = color;\n" +
            "}";

    /**
     * Create the instance of frame render filter
     * @param hue hue adjustment value
     */
    public HueFilter(float hue) {
        this(hue, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param hue hue adjustment value
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public HueFilter(float hue, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("hueAdjustment", hue)
                },
                transform);
    }
}
