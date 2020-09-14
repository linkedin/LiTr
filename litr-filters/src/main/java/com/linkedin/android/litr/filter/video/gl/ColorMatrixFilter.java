/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.graphics.PointF;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

public class ColorMatrixFilter extends BaseFrameRenderFilter {

    private static final String COLOR_MATRIX_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "uniform lowp samplerExternalOES sTexture;\n" +
            "uniform lowp mat4 matrix;\n" +
            "uniform lowp float intensity;\n" +

            "void main()\n" +
            "{\n" +
                "vec4 textureColor = texture2D(sTexture,vTextureCoord);\n" +
                "vec4 outputColor = textureColor * matrix;\n" +
                "gl_FragColor = mix(textureColor, outputColor, intensity);\n" +
            "}";

    private float intensity;
    private float[] colorMatrix4x4; // size = 16; (i.e. 4 x 4)

    /**
     * Create ColorMatrix frame render filter
     *
     * @param colorMatrix4x4 contains the color information (i.e. r,g,b,a) from 0.0 to 1.0
     * @param intensity      value, from range 0.0 to 1.0;
     */
    public ColorMatrixFilter(float[] colorMatrix4x4, float intensity) {
        super(DEFAULT_VERTEX_SHADER, COLOR_MATRIX_FRAGMENT_SHADER);

        this.intensity = intensity;
        this.colorMatrix4x4 = colorMatrix4x4;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     *
     * @param colorMatrix4x4 matrix of 4x4, contains the color information (i.e. r,g,b,a) from 0.0 to 1.0
     * @param intensity      value, from range 0.0 to 1.0;
     * @param size           size in X and Y direction, relative to target video frame
     * @param position       position of source video frame  center, in relative coordinate in 0 - 1 range
     *                       in fourth quadrant (0,0 is top left corner)
     * @param rotation       rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public ColorMatrixFilter(float[] colorMatrix4x4, float intensity, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, COLOR_MATRIX_FRAGMENT_SHADER, size, position, rotation);

        this.intensity = intensity;
        this.colorMatrix4x4 = colorMatrix4x4;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("intensity"), intensity);
        GLES20.glUniformMatrix4fv(getHandle("matrix"), 1, false, colorMatrix4x4, 0);
    }
}
