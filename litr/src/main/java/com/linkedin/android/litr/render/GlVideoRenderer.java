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
package com.linkedin.android.litr.render;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.Surface;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.codec.Frame;
import com.linkedin.android.litr.filter.GlFilter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A renderer that uses OpenGL to draw (and transform) decoder's output frame onto encoder's input frame. Both decoder
 * and encoder are expected to be using {@link Surface}.
 */
public class GlVideoRenderer implements VideoRenderer {

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

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

    private VideoRenderInputSurface inputSurface;
    private VideoRenderOutputSurface outputSurface;
    private List<GlFilter> filters;

    private FloatBuffer triangleVertices;
    private final float[] triangleVerticesData = {
        // X, Y, Z, U, V
        -1.0f, -1.0f, 0, 0.f, 0.f,
        1.0f, -1.0f, 0, 1.f, 0.f,
        -1.0f, 1.0f, 0, 0.f, 1.f,
        1.0f, 1.0f, 0, 1.f, 1.f,
        };

    private float[] mvpMatrix = new float[16];
    private float[] stMatrix = new float[16];

    private int glProgram;
    private int mvpMatrixHandle;
    private int uStMatrixHandle;
    private int aPositionHandle;
    private int aTextureHandle;

    /**
     * Create an instance of GlVideoRenderer
     * @param filters optional list of OpenGL filters to applied to output video frames
     */
    public GlVideoRenderer(@Nullable List<GlFilter> filters) {
        this.filters = filters == null ? Collections.<GlFilter>emptyList() : filters;

        triangleVertices = ByteBuffer.allocateDirect(
            triangleVerticesData.length * FLOAT_SIZE_BYTES)
                                     .order(ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(triangleVerticesData).position(0);
        Matrix.setIdentityM(stMatrix, 0);
    }

    @Override
    public void init(@Nullable Surface outputSurface, int rotation) {
        if (outputSurface == null) {
            throw new IllegalArgumentException("GlRenderer requires an output surface");
        }

        this.outputSurface = new VideoRenderOutputSurface(outputSurface);

        inputSurface = new VideoRenderInputSurface();
        initGl();
        initMvpMatrix(rotation);

        for (GlFilter filter : filters) {
            filter.init(Arrays.copyOf(mvpMatrix, mvpMatrix.length), 0);
        }
    }

    @Override
    @Nullable
    public Surface getInputSurface() {
        if (inputSurface != null) {
            return inputSurface.getSurface();
        }
        return null;
    }

    @Override
    public void renderFrame(@Nullable Frame frame, long presentationTimeNs) {
        inputSurface.awaitNewImage();
        drawFrame(presentationTimeNs);
        outputSurface.setPresentationTime(presentationTimeNs);
        outputSurface.swapBuffers();
    }

    @Override
    public void release() {
        inputSurface.release();
        outputSurface.release();
    }

    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    private void initGl() {
        glProgram = GlRenderUtils.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
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

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    private void drawFrame(long presentationTimeNs) {
        GlRenderUtils.checkGlError("onDrawFrame start");
        inputSurface.getTransformMatrix(stMatrix);
        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(glProgram);
        GlRenderUtils.checkGlError("glUseProgram");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputSurface.getTextureId());
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
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(uStMatrixHandle, 1, false, stMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GlRenderUtils.checkGlError("glDrawArrays");

        for (GlFilter filter : filters) {
            filter.apply(presentationTimeNs);
        }

        GLES20.glFinish();
    }

    private void initMvpMatrix(int rotation) {
        Matrix.setIdentityM(mvpMatrix, 0);

        // position the camera to match video frame rotation
        float upX;
        float upY;
        switch (rotation) {
            case 0:
                upX = 0;
                upY = 1;
                break;
            case 90:
                upX = 1;
                upY = 0;
                break;
            case 180:
                upX = 0;
                upY = -1;
                break;
            case 270:
                upX = -1;
                upY = 0;
                break;
            default:
                // this should never happen, but if it does, use trig as a last resort
                upX = (float) Math.sin(rotation / Math.PI);
                upY = (float) Math.cos(rotation / Math.PI);
                break;
        }
        Matrix.setLookAtM(mvpMatrix, 0,
                          0, 0, 1,
                          0, 0, 0,
                          upX, upY, 0);
    }
}
