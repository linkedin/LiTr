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
 * Performs a vignetting effect, fading out the video frame at the edges
 * x:
 * y: The directional intensity of the vignetting, with a default of x = 0.75, y = 0.5
 */
public class VignetteFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "uniform samplerExternalOES sTexture;\n" +
            "varying highp vec2 vTextureCoord;\n" +

            "uniform lowp vec2 vignetteCenter;\n" +
            "uniform lowp vec3 vignetteColor;\n" +
            "uniform highp float vignetteStart;\n" +
            "uniform highp float vignetteEnd;\n" +

            "void main()\n" +
            "{\n" +
                "lowp vec3 rgb = texture2D(sTexture, vTextureCoord).rgb;\n" +
                "lowp float d = distance(vTextureCoord, vec2(vignetteCenter.x, vignetteCenter.y));\n" +
                "lowp float percent = smoothstep(vignetteStart, vignetteEnd, d);\n" +
                "gl_FragColor = vec4(mix(rgb.x, vignetteColor.x, percent), mix(rgb.y, vignetteColor.y, percent), mix(rgb.z, vignetteColor.z, percent), 1.0);\n" +
            "}";

    private PointF vignetteCenter;
    private float[] vignetteColor;
    private float vignetteStart;
    private float vignetteEnd;

    /**
     * Create the instance of frame render filter
     * @param center center location, in relative coordinates
     * @param color RGB color
     * @param start start intensity
     * @param end end intensity
     */
    public VignetteFilter(@NonNull PointF center, @NonNull float[] color, float start, float end) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.vignetteCenter = center;
        this.vignetteColor = color;
        this.vignetteStart = start;
        this.vignetteEnd = end;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param center center location, in relative coordinates
     * @param color RGB color
     * @param start start intensity
     * @param end end intensity
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public VignetteFilter(@NonNull PointF center, @NonNull float[] color, float start, float end, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, size, position, rotation);

        this.vignetteCenter = center;
        this.vignetteColor = color;
        this.vignetteStart = start;
        this.vignetteEnd = end;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform2f(getHandle("vignetteCenter"), vignetteCenter.x, vignetteCenter.y);
        GLES20.glUniform3f(getHandle("vignetteColor"), vignetteColor[0], vignetteColor[1], vignetteColor[2]);
        GLES20.glUniform1f(getHandle("vignetteStart"), vignetteStart);
        GLES20.glUniform1f(getHandle("vignetteEnd"), vignetteEnd);
    }
}
