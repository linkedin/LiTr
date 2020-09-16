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
 * Created by edward_chiang on 13/10/16.
 */
package com.linkedin.android.litr.filter.video.gl;

import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.parameter.ShaderParameter;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform1i;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform3fv;

public class ColorBalanceFilter extends VideoFrameRenderFilter {

    private static final String COLOR_MATRIX_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "varying highp vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform lowp vec3 shadowsShift;\n" +
            "uniform lowp vec3 midtonesShift;\n" +
            "uniform lowp vec3 highlightsShift;\n" +
            "uniform int preserveLuminosity;\n" +
            "lowp vec3 RGBToHSL(lowp vec3 color)\n" +

            "{\n" +
            "lowp vec3 hsl; // init to 0 to avoid warnings ? (and reverse if + remove first part)\n" +

            "lowp float fmin = min(min(color.r, color.g), color.b);    //Min. value of RGB\n" +
            "lowp float fmax = max(max(color.r, color.g), color.b);    //Max. value of RGB\n" +
            "lowp float delta = fmax - fmin;             //Delta RGB value\n" +

            "hsl.z = (fmax + fmin) / 2.0; // Luminance\n" +

            "if (delta == 0.0)		//This is a gray, no chroma...\n" +
            "{\n" +
            "    hsl.x = 0.0;	// Hue\n" +
            "    hsl.y = 0.0;	// Saturation\n" +
            "}\n" +
            "else                                    //Chromatic data...\n" +
            "{\n" +
                "if (hsl.z < 0.5)\n" +
                    "hsl.y = delta / (fmax + fmin); // Saturation\n" +
                "else\n" +
                    "hsl.y = delta / (2.0 - fmax - fmin); // Saturation\n" +
            "\n" +
                "lowp float deltaR = (((fmax - color.r) / 6.0) + (delta / 2.0)) / delta;\n" +
                "lowp float deltaG = (((fmax - color.g) / 6.0) + (delta / 2.0)) / delta;\n" +
                "lowp float deltaB = (((fmax - color.b) / 6.0) + (delta / 2.0)) / delta;\n" +
            "\n" +
                "if (color.r == fmax )\n" +
                    "hsl.x = deltaB - deltaG; // Hue\n" +
                "else if (color.g == fmax)\n" +
                    "hsl.x = (1.0 / 3.0) + deltaR - deltaB; // Hue\n" +
                "else if (color.b == fmax)\n" +
                    "hsl.x = (2.0 / 3.0) + deltaG - deltaR; // Hue\n" +

                "if (hsl.x < 0.0)\n" +
                    "hsl.x += 1.0; // Hue\n" +
                "else if (hsl.x > 1.0)\n" +
                    "hsl.x -= 1.0; // Hue\n" +
            "}\n" +

            "return hsl;\n" +
            "}\n" +

            "lowp float HueToRGB(lowp float f1, lowp float f2, lowp float hue)\n" +
            "{\n" +
                "if (hue < 0.0)\n" +
                    "hue += 1.0;\n" +
                "else if (hue > 1.0)\n" +
                    "hue -= 1.0;\n" +
                "lowp float res;\n" +
                "if ((6.0 * hue) < 1.0)\n" +
                    "res = f1 + (f2 - f1) * 6.0 * hue;\n" +
                "else if ((2.0 * hue) < 1.0)\n" +
                    "res = f2;\n" +
                "else if ((3.0 * hue) < 2.0)\n" +
                    "res = f1 + (f2 - f1) * ((2.0 / 3.0) - hue) * 6.0;\n" +
                "else\n" +
                    "res = f1;\n" +
                "return res;\n" +
            "}\n" +

            "lowp vec3 HSLToRGB(lowp vec3 hsl)\n" +
            "{\n" +
                "lowp vec3 rgb;\n" +

                "if (hsl.y == 0.0)\n" +
                    "rgb = vec3(hsl.z); // Luminance\n" +
                "else\n" +
                "{\n" +
                    "lowp float f2;\n" +

                    "if (hsl.z < 0.5)\n" +
                        "f2 = hsl.z * (1.0 + hsl.y);\n" +
                    "else\n" +
                        "f2 = (hsl.z + hsl.y) - (hsl.y * hsl.z);\n" +

                    "lowp float f1 = 2.0 * hsl.z - f2;\n" +

                    "rgb.r = HueToRGB(f1, f2, hsl.x + (1.0/3.0));\n" +
                    "rgb.g = HueToRGB(f1, f2, hsl.x);\n" +
                    "rgb.b= HueToRGB(f1, f2, hsl.x - (1.0/3.0));\n" +
                "}\n" +

                "return rgb;\n  " +
            "}\n" +

            "lowp float RGBToL(lowp vec3 color)\n" +
            "{\n" +
                "lowp float fmin = min(min(color.r, color.g), color.b);    //Min. value of RGB\n" +
                "lowp float fmax = max(max(color.r, color.g), color.b);    //Max. value of RGB\n" +

                "return (fmax + fmin) / 2.0; // Luminance\n" +
            "}\n" +

            "void main()\n" +
            "{\n" +
                "lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +

                "// Alternative way:\n" +
                "//lowp vec3 lightness = RGBToL(textureColor.rgb);\n" +
                "lowp vec3 lightness = textureColor.rgb;\n" +

                "const lowp float a = 0.25;\n" +
                "const lowp float b = 0.333;\n" +
                "const lowp float scale = 0.7;\n" +

                "lowp vec3 shadows = shadowsShift * (clamp((lightness - b) / -a + 0.5, 0.0, 1.0) * scale);\n" +
                "lowp vec3 midtones = midtonesShift * (clamp((lightness - b) / a + 0.5, 0.0, 1.0) *\n" +
                    "clamp((lightness + b - 1.0) / -a + 0.5, 0.0, 1.0) * scale);\n" +
                "lowp vec3 highlights = highlightsShift * (clamp((lightness + b - 1.0) / a + 0.5, 0.0, 1.0) * scale);\n" +

                "mediump vec3 newColor = textureColor.rgb + shadows + midtones + highlights;\n" +
                "newColor = clamp(newColor, 0.0, 1.0);\n    " +

                "if (preserveLuminosity != 0)\n" +
                "{\n   " +
                    "lowp vec3 newHSL = RGBToHSL(newColor);\n" +
                    "lowp float oldLum = RGBToL(textureColor.rgb);\n" +
                    "textureColor.rgb = HSLToRGB(vec3(newHSL.x, newHSL.y, oldLum));\n" +
                    "gl_FragColor = textureColor;\n" +
                "}\n" +
                "else\n" +
                "{\n" +
                    "gl_FragColor = vec4(newColor.rgb, textureColor.w);\n" +
                "}\n" +
            "}\n";

    /**
     * Create frame render filter
     *
     * @param shadows shadow colors adjustment
     * @param midtones midtone colors adjustment
     * @param highlights highlight colors adjustment
     * @param preserveLuminosity flag indicating if luminosity should be preserved
     */
    public ColorBalanceFilter(float[] shadows, float[] midtones, float[] highlights, boolean preserveLuminosity) {
        this(shadows, midtones, highlights, preserveLuminosity, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     *
     * @param shadows shadow colors adjustment
     * @param midtones midtone colors adjustment
     * @param highlights highlight colors adjustment
     * @param preserveLuminosity flag indicating if luminosity should be preserved
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public ColorBalanceFilter(float[] shadows, float[] midtones, float[] highlights, boolean preserveLuminosity, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                COLOR_MATRIX_FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform3fv("shadowsShift", 1, shadows),
                        new Uniform3fv("midtonesShift", 1, midtones),
                        new Uniform3fv("highlightsShift", 1, highlights),
                        new Uniform1i("preserveLuminosity", preserveLuminosity ? 1 : 0)
                },
                transform);
    }
}
