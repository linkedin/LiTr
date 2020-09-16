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
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform1i;

/**
 * Kuwahara image abstraction, drawn from the work of Kyprianidis, et. al. in their publication
 * "Anisotropic Kuwahara Filtering on the GPU" within the GPU Pro collection. This produces an oil-painting-like
 * image, but it is extremely computationally expensive, so video transformation can be slow.
 */
public class KuwaharaFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision highp float;\n" +

            "uniform samplerExternalOES sTexture;" +
            "varying highp vec2 vTextureCoord;\n" +

            "uniform int radius;\n" +

            "const vec2 src_size = vec2 (1.0 / 768.0, 1.0 / 1024.0);\n" +

            "void main (void)\n" +
            "{\n" +
                "vec2 uv = vTextureCoord;\n" +
                "float n = float((radius + 1) * (radius + 1));\n" +
                "int i; int j;\n" +
                "vec3 m0 = vec3(0.0); vec3 m1 = vec3(0.0); vec3 m2 = vec3(0.0); vec3 m3 = vec3(0.0);\n" +
                "vec3 s0 = vec3(0.0); vec3 s1 = vec3(0.0); vec3 s2 = vec3(0.0); vec3 s3 = vec3(0.0);\n" +
                "vec3 c;\n" +
                "\n" +
                "for (j = -radius; j <= 0; ++j)  {\n" +
                    "for (i = -radius; i <= 0; ++i)  {\n" +
                        "c = texture2D(sTexture, uv + vec2(i,j) * src_size).rgb;\n" +
                        "m0 += c;\n" +
                        "s0 += c * c;\n" +
                    "}\n" +
                "}\n" +

                "for (j = -radius; j <= 0; ++j)  {\n" +
                    "for (i = 0; i <= radius; ++i)  {\n" +
                        "c = texture2D(sTexture, uv + vec2(i,j) * src_size).rgb;\n" +
                        "m1 += c;\n" +
                        "s1 += c * c;\n" +
                    "}\n" +
                "}\n" +

                "for (j = 0; j <= radius; ++j)  {\n" +
                    "for (i = 0; i <= radius; ++i)  {\n" +
                        "c = texture2D(sTexture, uv + vec2(i,j) * src_size).rgb;\n" +
                        "m2 += c;\n" +
                        "s2 += c * c;\n" +
                    "}\n" +
                "}\n" +

                "for (j = 0; j <= radius; ++j)  {\n" +
                    "for (i = -radius; i <= 0; ++i)  {\n" +
                        "c = texture2D(sTexture, uv + vec2(i,j) * src_size).rgb;\n" +
                        "m3 += c;\n" +
                        "s3 += c * c;\n" +
                    "}\n" +
                "}\n" +

                "float min_sigma2 = 1e+2;\n" +
                "m0 /= n;\n" +
                "s0 = abs(s0 / n - m0 * m0);\n" +

                "float sigma2 = s0.r + s0.g + s0.b;\n" +
                "if (sigma2 < min_sigma2)\n" +
                "{\n" +
                    "min_sigma2 = sigma2;\n" +
                    "gl_FragColor = vec4(m0, 1.0);\n" +
                "}\n" +

                "m1 /= n;\n" +
                "s1 = abs(s1 / n - m1 * m1);\n" +

                "sigma2 = s1.r + s1.g + s1.b;\n" +
                "if (sigma2 < min_sigma2)\n" +
                "{\n" +
                    "min_sigma2 = sigma2;\n" +
                    "gl_FragColor = vec4(m1, 1.0);\n" +
                "}\n" +

                "m2 /= n;\n" +
                "s2 = abs(s2 / n - m2 * m2);\n" +

                "sigma2 = s2.r + s2.g + s2.b;\n" +
                "if (sigma2 < min_sigma2)\n" +
                "{\n" +
                    "min_sigma2 = sigma2;\n" +
                    "gl_FragColor = vec4(m2, 1.0);\n" +
                "}\n" +

                "m3 /= n;\n" +
                "s3 = abs(s3 / n - m3 * m3);\n" +

                "sigma2 = s3.r + s3.g + s3.b;\n" +
                "if (sigma2 < min_sigma2)\n" +
                "{\n" +
                    "min_sigma2 = sigma2;\n" +
                    "gl_FragColor = vec4(m3, 1.0);\n" +
                "}\n" +
            "}\n";

    /**
     * Create the instance of frame render filter
     * @param radius filter radius
     */
    public KuwaharaFilter(int radius) {
        this(radius, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param radius filter radius
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public KuwaharaFilter(int radius, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1i("radius", radius)
                },
                transform);
    }

}
