/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// from: https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/TextureRender.java
// blob: 4125dcfcfed6ed7fddba5b71d657dec0d433da6a
// modified: removed unused method bodies
// modified: use GL_LINEAR for GL_TEXTURE_MIN_FILTER to improve quality.
// modified: added filters
package com.linkedin.android.litr.filter.video.gl;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import androidx.annotation.NonNull;

import com.linkedin.android.litr.filter.GlFrameRenderFilter;
import com.linkedin.android.litr.render.GlRenderUtils;

public class ScaleToFitGlFrameRenderFilter implements GlFrameRenderFilter {

    // shaders
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +      // highp here doesn't seem to matter
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    private float[] mvpMatrix = new float[16];
    private float[] inputFrameTextureMatrix = new float[16];

    private int glProgram;
    private int mvpMatrixHandle;
    private int uStMatrixHandle;
    private int inputFrameTextureHandle;

    @Override
    public void init(@NonNull float[] vpMatrix, int vpMatrixOffset) {
        Matrix.setIdentityM(inputFrameTextureMatrix, 0);

        mvpMatrix = vpMatrix;

        // Let's use features of VP matrix to extract frame aspect ratio from it
        // and scale source video frame to match the size of target video frame
        float videoAspectRatio;
        if (vpMatrix[0] == 0) {
            // portrait video
            videoAspectRatio = 1 / Math.abs(vpMatrix[4]);
            Matrix.scaleM(mvpMatrix, 0, 1, videoAspectRatio, 1);
        } else {
            // landscape video
            videoAspectRatio = 1 / Math.abs(vpMatrix[0]);
            Matrix.scaleM(mvpMatrix, 0, videoAspectRatio, 1, 1);
        }

        initGl();
    }

    @Override
    public void initInputFrameTexture(int textureHandle, @NonNull float[] transformMatrix) {
        inputFrameTextureHandle = textureHandle;
        inputFrameTextureMatrix = transformMatrix;
    }

    @Override
    public void apply(long presentationTimeNs) {
        GlRenderUtils.checkGlError("onDrawFrame start");
        GLES20.glUseProgram(glProgram);
        GlRenderUtils.checkGlError("glUseProgram");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputFrameTextureHandle);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(uStMatrixHandle, 1, false, inputFrameTextureMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GlRenderUtils.checkGlError("glDrawArrays");
    }

    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    private void initGl() {
        glProgram = GlRenderUtils.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (glProgram == 0) {
            throw new RuntimeException("failed creating glProgram");
        }
        mvpMatrixHandle = GLES20.glGetUniformLocation(glProgram, "uMVPMatrix");
        GlRenderUtils.checkGlError("glGetUniformLocation uMVPMatrix");
        if (mvpMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }
        uStMatrixHandle = GLES20.glGetUniformLocation(glProgram, "uSTMatrix");
        GlRenderUtils.checkGlError("glGetUniformLocation uSTMatrix");
        if (uStMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }
    }
}
