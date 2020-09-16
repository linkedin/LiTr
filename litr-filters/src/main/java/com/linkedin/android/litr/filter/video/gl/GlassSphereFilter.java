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

/**
 * Frame render filter that applies a glass sphere effect to video frame
 */
public class GlassSphereFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +

            "uniform highp vec2 center;\n" +
            "uniform highp float radius;\n" +
            "uniform highp float aspectRatio;\n" +
            "uniform highp float refractiveIndex;\n" +

            "const highp vec3 lightPosition = vec3(-0.5, 0.5, 1.0);\n" +
            "const highp vec3 ambientLightPosition = vec3(0.0, 0.0, 1.0);\n" +

            "void main()\n" +
            "{\n" +
                "highp vec2 textureCoordinateToUse = vec2(vTextureCoord.x, (vTextureCoord.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
                "highp float distanceFromCenter = distance(center, textureCoordinateToUse);\n" +
                "lowp float checkForPresenceWithinSphere = step(distanceFromCenter, radius);\n" +

                "distanceFromCenter = distanceFromCenter / radius;\n" +

                "highp float normalizedDepth = radius * sqrt(1.0 - distanceFromCenter * distanceFromCenter);\n" +
                "highp vec3 sphereNormal = normalize(vec3(textureCoordinateToUse - center, normalizedDepth));\n" +

                "highp vec3 refractedVector = 2.0 * refract(vec3(0.0, 0.0, -1.0), sphereNormal, refractiveIndex);\n" +
                "refractedVector.xy = -refractedVector.xy;\n" +

                "highp vec3 finalSphereColor = texture2D(sTexture, (refractedVector.xy + 1.0) * 0.5).rgb;\n" +

                "// Grazing angle lighting\n" +
                "highp float lightingIntensity = 2.5 * (1.0 - pow(clamp(dot(ambientLightPosition, sphereNormal), 0.0, 1.0), 0.25));\n" +
                "finalSphereColor += lightingIntensity;\n" +

                "// Specular lighting\n" +
                "lightingIntensity  = clamp(dot(normalize(lightPosition), sphereNormal), 0.0, 1.0);\n" +
                "lightingIntensity  = pow(lightingIntensity, 15.0);\n" +
                "finalSphereColor += vec3(0.8, 0.8, 0.8) * lightingIntensity;\n" +

                "gl_FragColor = vec4(finalSphereColor, 1.0) * checkForPresenceWithinSphere;\n" +
            "}";

    /**
     * Create frame render filter
     * @param center center of distortion, in relative coordinates in 0 - 1 range
     * @param radius radius of distortion, in relative coordinates in 0 - 1 range
     * @param aspectRatio aspect ratio of distortion
     * @param refractiveIndex refractive index
     */
    public GlassSphereFilter(@NonNull PointF center, float radius, float aspectRatio, float refractiveIndex) {
        this(center, radius, aspectRatio, refractiveIndex, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param center center of distortion, in relative coordinates in 0 - 1 range
     * @param radius radius of distortion, in relative coordinates in 0 - 1 range
     * @param aspectRatio aspect ratio of distortion
     * @param refractiveIndex refractive index
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public GlassSphereFilter(@NonNull PointF center, float radius, float aspectRatio, float refractiveIndex, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform2f("center", center.x, center.y),
                        new Uniform1f("radius", radius),
                        new Uniform1f("aspectRatio", aspectRatio),
                        new Uniform1f("refractiveIndex", refractiveIndex)
                },
                transform);
    }
}
