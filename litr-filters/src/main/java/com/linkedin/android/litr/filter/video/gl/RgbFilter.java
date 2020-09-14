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
 * Frame render filter that adjusts individual RGB channels
 */
public class RgbFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;" +
            "varying vec2 vTextureCoord;\n" +

            "uniform samplerExternalOES sTexture;\n" +
            "uniform highp float red;\n" +
            "uniform highp float green;\n" +
            "uniform highp float blue;\n" +

            "void main()\n" +
            "{\n" +
                "highp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
                "gl_FragColor = vec4(textureColor.r * red, textureColor.g * green, textureColor.b * blue, 1.0);\n" +
            "}\n";

    private float red;
    private float green;
    private float blue;

    /**
     * Create the instance of frame render filter
     * @param red red channel multiplier
     * @param green green channel multiplier
     * @param blue blue channel multiplier
     */
    public RgbFilter(float red, float green, float blue) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param red red channel multiplier
     * @param green green channel multiplier
     * @param blue blue channel multiplier
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public RgbFilter(float red, float green, float blue, @NonNull Transform transform) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, transform);

        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("red"), red);
        GLES20.glUniform1f(getHandle("green"), green);
        GLES20.glUniform1f(getHandle("blue"), blue);
    }
}
