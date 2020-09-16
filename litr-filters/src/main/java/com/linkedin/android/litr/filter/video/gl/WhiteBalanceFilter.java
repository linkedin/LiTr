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
 * Frame render filter that adjusts white balance
 */
public class WhiteBalanceFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "uniform samplerExternalOES sTexture;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform lowp float temperature;\n" +
            "uniform lowp float tint;\n" +
            "const lowp vec3 warmFilter = vec3(0.93, 0.54, 0.0);\n" +
            "const mediump mat3 RGBtoYIQ = mat3(0.299, 0.587, 0.114, 0.596, -0.274, -0.322, 0.212, -0.523, 0.311);\n" +
            "const mediump mat3 YIQtoRGB = mat3(1.0, 0.956, 0.621, 1.0, -0.272, -0.647, 1.0, -1.105, 1.702);\n" +

            "void main()\n" +
            "{\n" +
                "lowp vec4 source = texture2D(sTexture, vTextureCoord);\n" +
                "mediump vec3 yiq = RGBtoYIQ * source.rgb; //adjusting tint\n" +
                "yiq.b = clamp(yiq.b + tint*0.5226*0.1, -0.5226, 0.5226);\n" +
                "lowp vec3 rgb = YIQtoRGB * yiq;\n" +
                "lowp vec3 processed = vec3(\n" +
                    "(rgb.r < 0.5 ? (2.0 * rgb.r * warmFilter.r) : (1.0 - 2.0 * (1.0 - rgb.r) * (1.0 - warmFilter.r))), //adjusting temperature\n" +
                    "(rgb.g < 0.5 ? (2.0 * rgb.g * warmFilter.g) : (1.0 - 2.0 * (1.0 - rgb.g) * (1.0 - warmFilter.g))), \n" +
                    "(rgb.b < 0.5 ? (2.0 * rgb.b * warmFilter.b) : (1.0 - 2.0 * (1.0 - rgb.b) * (1.0 - warmFilter.b))));\n" +
                "gl_FragColor = vec4(mix(rgb, processed, temperature), source.a);\n" +
            "}";

    /**
     * Create the instance of frame render filter
     * @param temperature color temperature in K
     * @param tint tint value
     */
    public WhiteBalanceFilter(float temperature, float tint) {
        this(temperature, tint, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param temperature color temperature in K
     * @param tint tint value
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public WhiteBalanceFilter(float temperature, float tint, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("temperature",
                                temperature < 5000
                                ? 0.0004f * (temperature - 5000.0f)
                                : 0.00006f * (temperature - 5000.0f)),
                        new Uniform1f("tint", tint / 100f)
                },
                transform);
    }
}
