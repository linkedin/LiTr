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
 * Frame render filter that applies haze effect
 */
public class HazeFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform lowp float distance;\n" +
            "uniform highp float slope;\n" +

            "void main()\n" +
            "{\n" +
                "highp vec4 color = vec4(1.0);\n" +
                "highp float  d = vTextureCoord.y * slope  +  distance;\n" +
                "highp vec4 c = texture2D(sTexture, vTextureCoord);\n" +
                "c = (c - d * color) / (1.0 -d);\n" +
                "gl_FragColor = c;\n" +    // consider using premultiply(c);
            "}";

    private float distance;
    private float slope;

    /**
     * Create the instance of frame render filter
     * @param distance haze distance
     * @param slope haze slope
     */
    public HazeFilter(float distance, float slope) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.distance = distance;
        this.slope = slope;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param distance haze distance
     * @param slope haze slope
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public HazeFilter(float distance, float slope, @NonNull Transform transform) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, transform);

        this.distance = distance;
        this.slope = slope;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("distance"), distance);
        GLES20.glUniform1f(getHandle("slope"), slope);
    }
}
