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

import android.graphics.PointF;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.linkedin.android.litr.filter.Transform;

/**
 * Frame render filter that applies a sphere refraction effect to video frame
 */
public class SphereRefractionFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform highp vec2 center;\n" +
            "uniform highp float radius;\n" +
            "uniform highp float aspectRatio;\n" +
            "uniform highp float refractiveIndex;\n" +

            "void main()\n" +
            "{\n" +
                "highp vec2 textureCoordinateToUse = vec2(vTextureCoord.x, (vTextureCoord.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
                "highp float distanceFromCenter = distance(center, textureCoordinateToUse);\n" +
                "lowp float checkForPresenceWithinSphere = step(distanceFromCenter, radius);\n" +

                "distanceFromCenter = distanceFromCenter / radius;\n" +

                "highp float normalizedDepth = radius * sqrt(1.0 - distanceFromCenter * distanceFromCenter);\n" +
                "highp vec3 sphereNormal = normalize(vec3(textureCoordinateToUse - center, normalizedDepth));\n" +

                "highp vec3 refractedVector = refract(vec3(0.0, 0.0, -1.0), sphereNormal, refractiveIndex);\n" +

                "gl_FragColor = texture2D(sTexture, (refractedVector.xy + 1.0) * 0.5) * checkForPresenceWithinSphere;\n" +
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
    public SphereRefractionFilter(@NonNull PointF center, float radius, float aspectRatio, float refractiveIndex) {
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
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public SphereRefractionFilter(@NonNull PointF center, float radius, float aspectRatio, float refractiveIndex, @NonNull Transform transform) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, transform);

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
