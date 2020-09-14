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
 *
 * Created by vashisthg 30/05/14.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.linkedin.android.litr.filter.Transform;

/**
 * Adjust color levels of video pixels
 */
public class LevelsFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision lowp float;\n" +

            "uniform samplerExternalOES sTexture;\n" +
            "varying highp vec2 vTextureCoord;\n" +

            "uniform mediump vec3 levelMinimum;\n" +
            "uniform mediump vec3 levelMiddle;\n" +
            "uniform mediump vec3 levelMaximum;\n" +
            "uniform mediump vec3 minOutput;\n" +
            "uniform mediump vec3 maxOutput;\n" +

            "void main()\n" +
            "{\n" +
                "mediump vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
                "gl_FragColor = vec4( mix(minOutput, maxOutput, pow(min(max(textureColor.rgb - levelMinimum, vec3(0.0)) / (levelMaximum - levelMinimum), vec3(1.0)), 1.0 / levelMiddle)) , textureColor.a);\n" +
            "}\n";

    private float[] min;
    private float[] mid;
    private float[] max;
    private float[] minOutput;
    private float[] maxOutput;

    /**
     * Create the instance of frame render filter
     * @param min minimum level
     * @param mid mid level
     * @param max maximum level
     * @param minOutput minimum target color channel values
     * @param maxOutput maximum target color channel values
     */
    public LevelsFilter(@NonNull float[] min, @NonNull float[] mid, @NonNull float[] max, @NonNull float[] minOutput, @NonNull float[] maxOutput) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.min = min;
        this.mid = mid;
        this.max = max;
        this.minOutput = minOutput;
        this.maxOutput = maxOutput;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param min minimum level
     * @param mid mid level
     * @param max maximum level
     * @param minOutput minimum target color channel values
     * @param maxOutput maximum target color channel values
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public LevelsFilter(@NonNull float[] min, @NonNull float[] mid, @NonNull float[] max, @NonNull float[] minOutput, @NonNull float[] maxOutput, @NonNull Transform transform) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, transform);

        this.min = min;
        this.mid = mid;
        this.max = max;
        this.minOutput = minOutput;
        this.maxOutput = maxOutput;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform3f(getHandle("levelMinimum"), min[0], min[1], min[2]);
        GLES20.glUniform3f(getHandle("levelMiddle"), mid[0], mid[1], mid[2]);
        GLES20.glUniform3f(getHandle("levelMaximum"), max[0], max[1], max[2]);
        GLES20.glUniform3f(getHandle("minOutput"), minOutput[0], minOutput[1], minOutput[2]);
        GLES20.glUniform3f(getHandle("maxOutput"), maxOutput[0], maxOutput[1], maxOutput[2]);
    }
}
