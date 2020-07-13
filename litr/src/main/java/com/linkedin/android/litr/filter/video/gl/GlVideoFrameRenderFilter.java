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

import android.graphics.PointF;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import androidx.annotation.NonNull;

import com.linkedin.android.litr.filter.GlFrameRenderFilter;
import com.linkedin.android.litr.filter.util.GlFilterUtil;
import com.linkedin.android.litr.render.GlRenderUtils;
import com.linkedin.android.litr.shader.FragmentShaders;
import com.linkedin.android.litr.shader.VertexShaders;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Implementation of GlFrameRenderFilter, which renders source video frame onto target video frame,
 * optionally applying pixel and geometric transformation.
 */
public class GlVideoFrameRenderFilter implements GlFrameRenderFilter {

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    private final String vertexShader;
    private final String fragmentShader;
    private final PointF size;
    private final PointF position;
    private final float rotation;

    private float[] mvpMatrix = new float[16];
    private float[] inputFrameTextureMatrix = new float[16];
    private int mvpMatrixOffset;

    private FloatBuffer triangleVertices;
    private final float[] triangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f, 1.0f, 0, 0.f, 1.f,
            1.0f, 1.0f, 0, 1.f, 1.f,
    };

    private int glProgram;
    private int mvpMatrixHandle;
    private int uStMatrixHandle;
    private int inputFrameTextureHandle;
    private int aPositionHandle;
    private int aTextureHandle;

    /**
     * Create most basic filter, which scales source frame to fit target frame, with no pixel modification.
     */
    public GlVideoFrameRenderFilter() {
        this(VertexShaders.DEFAULT_SHADER, FragmentShaders.DEFAULT_SHADER);
    }

    /**
     * Create filter which scales source frame to fit target frame, apply vertex and frame shaders. Can be used for
     * things like pixel modification.
     * @param vertexShader vertex shader
     * @param fragmentShader fragment shader
     */
    public GlVideoFrameRenderFilter(@NonNull String vertexShader, @NonNull String fragmentShader) {
        this(vertexShader, fragmentShader, new PointF(1f, 1f), new PointF(0.5f, 0.5f), 0);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * No pixel data is modified.
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public GlVideoFrameRenderFilter(@NonNull PointF size, @NonNull PointF position, float rotation) {
        this(VertexShaders.DEFAULT_SHADER, FragmentShaders.DEFAULT_SHADER, size, position, rotation);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * Use provided vertex and fragment filter, to do things like pixel modification.
     * @param vertexShader vertex shader
     * @param fragmentShader fragment shader
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public GlVideoFrameRenderFilter(@NonNull String vertexShader,
                                    @NonNull String fragmentShader,
                                    @NonNull PointF size,
                                    @NonNull PointF position,
                                    float rotation) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.size = size;
        this.position = position;
        this.rotation = rotation;
    }

    @Override
    public void init(@NonNull float[] vpMatrix, int vpMatrixOffset) {
        Matrix.setIdentityM(inputFrameTextureMatrix, 0);

        mvpMatrix = vpMatrix;

        mvpMatrix = GlFilterUtil.createFilterMvpMatrix(vpMatrix, size, position, rotation);
        mvpMatrixOffset = vpMatrixOffset;

        triangleVertices = ByteBuffer.allocateDirect(
                triangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(triangleVerticesData).position(0);

        initGl();
    }

    @Override
    public void initInputFrameTexture(int textureHandle, @NonNull float[] transformMatrix) {
        inputFrameTextureHandle = textureHandle;
        inputFrameTextureMatrix = transformMatrix;
    }

    @Override
    public void apply(long presentationTimeNs) {
        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        GlRenderUtils.checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GlRenderUtils.checkGlError("glEnableVertexAttribArray aPositionHandle");
        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        GlRenderUtils.checkGlError("glVertexAttribPointer aTextureHandle");
        GLES20.glEnableVertexAttribArray(aTextureHandle);
        GlRenderUtils.checkGlError("glEnableVertexAttribArray aTextureHandle");

        GlRenderUtils.checkGlError("onDrawFrame start");
        GLES20.glUseProgram(glProgram);
        GlRenderUtils.checkGlError("glUseProgram");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputFrameTextureHandle);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, mvpMatrixOffset);
        GLES20.glUniformMatrix4fv(uStMatrixHandle, 1, false, inputFrameTextureMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GlRenderUtils.checkGlError("glDrawArrays");
    }

    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    private void initGl() {
        glProgram = GlRenderUtils.createProgram(vertexShader, fragmentShader);
        if (glProgram == 0) {
            throw new RuntimeException("failed creating glProgram");
        }
        aPositionHandle = GLES20.glGetAttribLocation(glProgram, "aPosition");
        GlRenderUtils.checkGlError("glGetAttribLocation aPosition");
        if (aPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        aTextureHandle = GLES20.glGetAttribLocation(glProgram, "aTextureCoord");
        GlRenderUtils.checkGlError("glGetAttribLocation aTextureCoord");
        if (aTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
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
