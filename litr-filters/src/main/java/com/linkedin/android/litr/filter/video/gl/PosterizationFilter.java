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
 * Frame render filter that applies posterization effect (color resolution reduction) to video pixels
 */
public class PosterizationFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform highp float colorLevelCount;\n" +

            "void main()\n" +
            "{\n" +
                "highp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
                "gl_FragColor = floor((textureColor * colorLevelCount) + vec4(0.5)) / colorLevelCount;\n" +
            "}";

    private float colorLevelCount;

    /**
     * Create the instance of frame render filter
     * @param colorLevelCount number of color levels
     */
    public PosterizationFilter(float colorLevelCount) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.colorLevelCount = colorLevelCount;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param colorLevelCount number of color levels
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public PosterizationFilter(float colorLevelCount, @NonNull Transform transform) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, transform);

        this.colorLevelCount = colorLevelCount;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("colorLevelCount"), colorLevelCount);
    }
}
