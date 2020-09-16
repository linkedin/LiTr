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
 * Frame render filter that converts video frame into a "cross hatch" rendering
 */
public class CrossHatchFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform highp float crossHatchSpacing;\n" +
            "uniform highp float lineWidth;\n" +
            "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +

            "void main()\n" +
            "{\n" +
                "highp float luminance = dot(texture2D(sTexture, vTextureCoord).rgb, W);\n" +
                "lowp vec4 colorToDisplay = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                "if (luminance < 1.00)\n" +
                "{\n" +
                    "if (mod(vTextureCoord.x + vTextureCoord.y, crossHatchSpacing) <= lineWidth)\n" +
                    "{\n" +
                        "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                    "}\n" +
                "}\n" +
                "if (luminance < 0.75)\n" +
                "{\n" +
                    "if (mod(vTextureCoord.x - vTextureCoord.y, crossHatchSpacing) <= lineWidth)\n" +
                    "{\n" +
                        "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                    "}\n" +
                "}\n" +
                "if (luminance < 0.50)\n" +
                "{\n" +
                    "if (mod(vTextureCoord.x + vTextureCoord.y - (crossHatchSpacing / 2.0), crossHatchSpacing) <= lineWidth)\n" +
                    "{\n" +
                        "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                    "}\n" +
                "}\n" +
                "if (luminance < 0.3)\n" +
                "{\n" +
                    "if (mod(vTextureCoord.x - vTextureCoord.y - (crossHatchSpacing / 2.0), crossHatchSpacing) <= lineWidth)\n" +
                    "{\n" +
                        "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                    "}\n" +
                "}\n" +
                "gl_FragColor = colorToDisplay;\n" +
            "}";

    /**
     * Create cross hatch filter
     * @param crossHatchSpacing spacing between cross hatches, in relative coordinates
     * @param lineWidth thickness of cross hatch line, in relative coordinates
     */
    public CrossHatchFilter(float crossHatchSpacing, float lineWidth) {
        this(crossHatchSpacing, lineWidth, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param crossHatchSpacing spacing between cross hatches, in relative coordinates
     * @param lineWidth thickness of cross hatch line, in relative coordinates
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public CrossHatchFilter(float crossHatchSpacing, float lineWidth, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("crossHatchSpacing", crossHatchSpacing),
                        new Uniform1f("lineWidth", lineWidth)
                },
                transform);
    }
}
