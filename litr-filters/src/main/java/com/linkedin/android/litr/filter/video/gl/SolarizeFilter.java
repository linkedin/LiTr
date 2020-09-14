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

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.linkedin.android.litr.filter.Transform;

/**
 * Frame render filter that applies solarize effect (switch dark and light tones) to video pixels
 */
public class SolarizeFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform highp float threshold;\n" +
            "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +

            "void main()\n" +
            "{\n" +
                "highp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
                "highp float luminance = dot(textureColor.rgb, W);\n" +
                "highp float thresholdResult = step(luminance, threshold);\n" +
                "highp vec3 finalColor = abs(thresholdResult - textureColor.rgb);\n" +
                "gl_FragColor = vec4(finalColor, textureColor.w);\n" +
            "}";

    private float threshold;

    /**
     * Create the instance of frame render filter
     * @param threshold threshold between dark and light colors, between 0 and 1
     */
    public SolarizeFilter(float threshold) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.threshold = threshold;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param threshold threshold between dark and light colors, between 0 and 1
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public SolarizeFilter(float threshold, @NonNull Transform transform) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, transform);

        this.threshold = threshold;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("threshold"), threshold);
    }
}
