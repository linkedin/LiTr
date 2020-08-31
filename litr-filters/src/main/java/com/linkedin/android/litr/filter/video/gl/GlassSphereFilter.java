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
 * Frame render filter that applies a glass sphere effect to video frame
 */
public class GlassSphereFilter extends BaseFrameRenderFilter {

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

    private PointF center;
    private float radius;
    private float aspectRatio;
    private float refractiveIndex;

    /**
     * Create frame render filter
     * @param center center of distortion, in relative coordinates in 0 - 1 range
     * @param radius radius of distortion, in relative coordinates in 0 - 1 range
     * @param aspectRatio aspect ratio of distortion
     * @param refractiveIndex refractive index
     */
    public GlassSphereFilter(@NonNull PointF center, float radius, float aspectRatio, float refractiveIndex) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.center = center;
        this.radius = radius;
        this.aspectRatio = aspectRatio;
        this.refractiveIndex = refractiveIndex;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param center center of distortion, in relative coordinates in 0 - 1 range
     * @param radius radius of distortion, in relative coordinates in 0 - 1 range
     * @param aspectRatio aspect ratio of distortion
     * @param refractiveIndex refractive index
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public GlassSphereFilter(@NonNull PointF center, float radius, float aspectRatio, float refractiveIndex, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, size, position, rotation);

        this.center = center;
        this.radius = radius;
        this.aspectRatio = aspectRatio;
        this.refractiveIndex = refractiveIndex;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform2f(getHandle("center"), center.x, center.y);
        GLES20.glUniform1f(getHandle("radius"), radius);
        GLES20.glUniform1f(getHandle("aspectRatio"), aspectRatio);
        GLES20.glUniform1f(getHandle("refractiveIndex"), refractiveIndex);
    }
}
