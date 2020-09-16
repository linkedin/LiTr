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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.parameter.ShaderParameter;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform1f;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform2f;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform3f;

/**
 * Performs a vignetting effect, fading out the video frame at the edges
 * x:
 * y: The directional intensity of the vignetting, with a default of x = 0.75, y = 0.5
 */
public class VignetteFilter extends VideoFrameRenderFilter {

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

    /**
     * Create the instance of frame render filter
     * @param center center location, in relative coordinates
     * @param color RGB color
     * @param start start intensity
     * @param end end intensity
     */
    public VignetteFilter(@NonNull PointF center, @NonNull float[] color, float start, float end) {
        this(center, color, start, end, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param center center location, in relative coordinates
     * @param color RGB color
     * @param start start intensity
     * @param end end intensity
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public VignetteFilter(@NonNull PointF center, @NonNull float[] color, float start, float end, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform2f("vignetteCenter", center.x, center.y),
                        new Uniform3f("vignetteColor", color[0], color[1], color[2]),
                        new Uniform1f("vignetteStart", start),
                        new Uniform1f("vignetteEnd", end)
                },
                transform);
    }
}
