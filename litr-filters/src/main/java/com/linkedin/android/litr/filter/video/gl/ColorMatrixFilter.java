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

import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.parameter.ShaderParameter;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform1f;
import com.linkedin.android.litr.filter.video.gl.parameter.UniformMatrix4fv;

public class ColorMatrixFilter extends VideoFrameRenderFilter {

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

    /**
     * Create ColorMatrix frame render filter
     *
     * @param colorMatrix4x4 contains the color information (i.e. r,g,b,a) from 0.0 to 1.0
     * @param intensity      value, from range 0.0 to 1.0;
     */
    public ColorMatrixFilter(float[] colorMatrix4x4, float intensity) {
        this(colorMatrix4x4, intensity, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     *
     * @param colorMatrix4x4 matrix of 4x4, contains the color information (i.e. r,g,b,a) from 0.0 to 1.0
     * @param intensity      value, from range 0.0 to 1.0;
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public ColorMatrixFilter(float[] colorMatrix4x4, float intensity, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                COLOR_MATRIX_FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("intensity", intensity),
                        new UniformMatrix4fv("matrix", 1, false, colorMatrix4x4, 0)
                },
                transform);
    }
}
