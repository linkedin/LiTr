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

import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.parameter.ShaderParameter;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform1f;

/**
 * Frame render filter that adjusts the saturation of video pixels
 */
public class VibranceFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            " varying vec2 vTextureCoord;\n" +
            " uniform samplerExternalOES sTexture;\n" +
            " uniform lowp float vibrance;\n" +

            "void main()\n" +
            "{\n" +
                "lowp vec4 color = texture2D(sTexture, vTextureCoord);\n" +
                "lowp float average = (color.r + color.g + color.b) / 3.0;\n" +
                "lowp float mx = max(color.r, max(color.g, color.b));\n" +
                "lowp float amt = (mx - average) * (-vibrance * 3.0);\n" +
                "color.rgb = mix(color.rgb, vec3(mx), amt);\n" +
                "gl_FragColor = color;\n" +
            "}";

    /**
     * Create the instance of frame render filter
     * @param vibrance vibrance adjustment value
     */
    public VibranceFilter(float vibrance) {
        this(vibrance, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param vibrance vibrance adjustment value
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public VibranceFilter(float vibrance, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("vibrance", vibrance)
                },
                transform);
    }

}
