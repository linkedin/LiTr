/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.render.GlRenderUtils;

abstract class BaseOverlayGlFilter implements GlFilter {

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

    private static final String FRAGMENT_OVERLAY_SHADER =
        "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(uTexture, vTextureCoord);\n" +
            "}\n";

    private final PointF position;
    private final PointF size;
    private final float rotation;

    private int glOverlayProgram;
    private int overlayMvpMatrixHandle;
    private int overlayUstMatrixHandle;

    private float[] mvpMatrix;
    private int mvpMatrixOffset;

    private float[] stMatrix = new float[16];

    BaseOverlayGlFilter(@Nullable RectF bitmapRect) {
        if (bitmapRect == null) {
            size = new PointF(1, 1);
            position = new PointF(0.5f, 0.5f);
        } else {
            size = new PointF(bitmapRect.right - bitmapRect.left, bitmapRect.bottom - bitmapRect.top);
            position = new PointF((bitmapRect.left + bitmapRect.right) / 2,
                                  (bitmapRect.top + bitmapRect.bottom) / 2);
        }
        rotation = 0;
    }

    BaseOverlayGlFilter(@NonNull PointF size, @NonNull PointF position, float rotation) {
        this.size = size;
        this.position = position;
        this.rotation = rotation;
    }

    @Override
    @CallSuper
    public void init(@NonNull float[] vpMatrix, int vpMatrixOffset) {
        Matrix.setIdentityM(stMatrix, 0);

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
            scaleY = size.y / videoAspectRatio;
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

        // Matrix operations in OpenGL are done in reverse. So here we scale (and flip vertically) first, then rotate
        // around the center, and then translate into desired position.
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, translateX, translateY, 0);
        Matrix.rotateM(modelMatrix, 0, rotation, 0, 0, 1);
        Matrix.scaleM(modelMatrix, 0, scaleX, -scaleY, 1);

        // last, we multiply the model matrix by the view matrix to get final MVP matrix for an overlay
        mvpMatrix = new float[16];
        mvpMatrixOffset = 0;
        Matrix.multiplyMM(this.mvpMatrix, this.mvpMatrixOffset, vpMatrix, 0, modelMatrix, 0);
    }

    void renderOverlayTexture(int textureId) {
        // Switch to overlay texture
        GLES20.glUseProgram(glOverlayProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glUniformMatrix4fv(overlayMvpMatrixHandle, 1, false, mvpMatrix, mvpMatrixOffset);
        GLES20.glUniformMatrix4fv(overlayUstMatrixHandle, 1, false, stMatrix, 0);

        // Enable blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Call OpenGL to draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GlRenderUtils.checkGlError("glDrawArrays");

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    /**
     * Create a texture and load the overlay bitmap into this texture.
     */
    int createOverlayTexture(@NonNull Bitmap overlayBitmap) {
        int overlayTextureID;

        // Create program
        glOverlayProgram = GlRenderUtils.createProgram(VERTEX_SHADER, FRAGMENT_OVERLAY_SHADER);
        if (glOverlayProgram == 0) {
            throw new RuntimeException("failed creating glOverlayProgram");
        }

        // Get the location of our uniforms
        overlayMvpMatrixHandle = GLES20.glGetUniformLocation(glOverlayProgram, "uMVPMatrix");
        GlRenderUtils.checkGlError("glGetUniformLocation uMVPMatrix");
        if (overlayMvpMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }
        overlayUstMatrixHandle = GLES20.glGetUniformLocation(glOverlayProgram, "uSTMatrix");
        GlRenderUtils.checkGlError("glGetUniformLocation uSTMatrix");
        if (overlayUstMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        // Generate one texture for overlay
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        overlayTextureID = textures[0];

        // Tell OpenGL to bind this texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTextureID);
        GlRenderUtils.checkGlError("glBindTexture overlayTextureID");

        // Set default texture filtering parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GlRenderUtils.checkGlError("glTexParameter");

        // Load the bitmap and copy it over into the texture
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, overlayBitmap, 0);

        return overlayTextureID;
    }
}
