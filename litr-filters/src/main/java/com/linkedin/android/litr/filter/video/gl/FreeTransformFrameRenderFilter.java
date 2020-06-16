/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.graphics.PointF;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import androidx.annotation.NonNull;

import com.linkedin.android.litr.filter.GlFrameRenderFilter;
import com.linkedin.android.litr.render.GlRenderUtils;

/**
 * Implementation of GlFrameRenderFilter which can transform (scale, rotate, translate)
 * source video frame when rendering it onto targetVideoFrame
 */
public class FreeTransformFrameRenderFilter implements GlFrameRenderFilter {

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
    private float[] inputTextureTransformMatrix = new float[16];
    private int mvpMatrixOffset;

    private int glProgram;
    private int mvpMatrixHandle;
    private int uStMatrixHandle;
    private int inputTextureId;

    private final PointF position;
    private final PointF size;
    private final float rotation;

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public FreeTransformFrameRenderFilter(@NonNull PointF size, @NonNull PointF position, float rotation) {
        this.size = size;
        this.position = position;
        this.rotation = rotation;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param videoRotation rotation (orientation) of source video frame, usually in increments of 90 degrees
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     * @deprecated use FreeTransformFrameRenderFilter(PointF, PointF, float)
     */
    @Deprecated
    public FreeTransformFrameRenderFilter(int videoRotation, @NonNull PointF size, @NonNull PointF position, float rotation) {
        this.size = size;
        this.position = position;
        this.rotation = rotation;
    }

    @Override
    public void init(@NonNull float[] vpMatrix, int vpMatrixOffset) {
        Matrix.setIdentityM(inputTextureTransformMatrix, 0);

        // Let's use features of VP matrix to extract frame aspect ratio and orientation from it
        // for 90 and 270 degree rotations (portrait orientation) top left element will be zero
        boolean isPortraitVideo = vpMatrix[0] == 0;

        // orthogonal projection matrix is basically a scaling matrix, which scales along X axis.
        // 0 and 180 degree rotations keep the scaling factor in top left element (they don't move it)
        // 90 and 270 degree rotations move it to one position right in top row
        // Inverting scaling factor gives us the aspect ratio.
        // Scale can be negative if video is flipped, so we use absolute value.
        float videoAspectRatio;
        if (isPortraitVideo) {
            videoAspectRatio = 1 / Math.abs(vpMatrix[4]);
        } else {
            videoAspectRatio = 1 / Math.abs(vpMatrix[0]);
        }

        // Size is respective to video frame, and frame will later be scaled by perspective and view matrices.
        // So we have to adjust the scale accordingly.
        float scaleX;
        float scaleY;
        if (isPortraitVideo) {
            scaleX = size.x;
            scaleY = size.y * videoAspectRatio;
        } else {
            scaleX = size.x * videoAspectRatio;
            scaleY = size.y;
        }

        // Position values are in relative (0, 1) range, which means they have to be mapped from (-1, 1) range
        // and adjusted for aspect ratio.
        float translateX;
        float translateY;
        if (isPortraitVideo) {
            translateX = position.x * 2 - 1;
            translateY = (1 - position.y * 2) * videoAspectRatio;
        } else {
            translateX = (position.x * 2 - 1) * videoAspectRatio;
            translateY = 1 - position.y * 2;
        }

        // Matrix operations in OpenGL are done in reverse.
        // First, we have to rotate the video into correct orientation (portrait/landscape)
        // Then, we scale (and flip vertically) first, then rotate
        // around the center, and then translate into desired position.
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, translateX, translateY, 0);
        Matrix.rotateM(modelMatrix, 0, rotation, 0, 0, 1);
        Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1);

        // last, we multiply the model matrix by the view matrix to get final MVP matrix for an overlay
        mvpMatrix = new float[16];
        mvpMatrixOffset = vpMatrixOffset;
        Matrix.multiplyMM(this.mvpMatrix, this.mvpMatrixOffset, vpMatrix, 0, modelMatrix, 0);

        initGl();
    }

    @Override
    public void initInputFrameTexture(int textureId, @NonNull float[] transformMatrix) {
        inputTextureId = textureId;
        inputTextureTransformMatrix = transformMatrix;
    }

    @Override
    public void apply(long presentationTimeNs) {
        GlRenderUtils.checkGlError("onDrawFrame start");
        GLES20.glUseProgram(glProgram);
        GlRenderUtils.checkGlError("glUseProgram");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputTextureId);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, mvpMatrixOffset);
        GLES20.glUniformMatrix4fv(uStMatrixHandle, 1, false, inputTextureTransformMatrix, 0);

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
