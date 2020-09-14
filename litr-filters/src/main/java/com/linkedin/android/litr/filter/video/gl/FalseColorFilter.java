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

/**
 * Colors video pixels in false colors. A color channel value of a pixel is calculated as an interpolation between
 * two channel values weighted by video pixel luminance.
 */
public class FalseColorFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision lowp float;\n" +

            "uniform samplerExternalOES sTexture;\n" +
            "varying highp vec2 vTextureCoord;\n" +

            "uniform float intensity;\n" +
            "uniform vec3 firstColor;\n" +
            "uniform vec3 secondColor;\n" +

            "const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +

            "void main()\n" +
            "{\n" +
                "lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
                "float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
                "gl_FragColor = vec4( mix(firstColor.rgb, secondColor.rgb, luminance), textureColor.a);\n" +
            "}\n";

    private float[] firstColor;
    private float[] secondColor;

    /**
     * Create the instance of frame render filter
     * @param firstColor first color channel values
     * @param secondColor second color channel values
     */
    public FalseColorFilter(@NonNull float[] firstColor, @NonNull float[] secondColor) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.firstColor = firstColor;
        this.secondColor = secondColor;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param firstColor first color channel values
     * @param secondColor second color channel values
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public FalseColorFilter(@NonNull float[] firstColor, @NonNull float[] secondColor, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, size, position, rotation);

        this.firstColor = firstColor;
        this.secondColor = secondColor;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform3f(getHandle("firstColor"), firstColor[0], firstColor[1], firstColor[2]);
        GLES20.glUniform3f(getHandle("secondColor"), secondColor[0], secondColor[1], secondColor[2]);
    }
}
