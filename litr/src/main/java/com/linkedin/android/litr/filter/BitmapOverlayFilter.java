/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.render.GlRenderUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * An OpenGL filter that overlays a static bitmap on top of all video frames.
 */
public class BitmapOverlayFilter implements GlFilter {

    private static final String TAG = BitmapOverlayFilter.class.getSimpleName();

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

    private final Context context;
    private final Uri bitmapUri;
    private final RectF bitmapRect;

    private int glOverlayProgram;
    private int overlayTextureID = -12346;
    private int overlayMvpMatrixHandle;
    private int overlayUstMatrixHandle;

    private float[] mvpMatrix;
    private int mvpMatrixOffset;

    private float[] stMatrix = new float[16];

    /**
     * Create filter with certain configuration.
     * @param context context for accessing bitmap
     * @param bitmapUri bitmap {@link Uri}
     * @param bitmapRect Rectangle of bitmap's target position on a video frame, in relative coordinate in 0 - 1 range
     *                   in fourth quadrant (0,0 is top left corner)
     */
    public BitmapOverlayFilter(@NonNull Context context, @NonNull Uri bitmapUri, @Nullable RectF bitmapRect) {
        this.context = context;
        this.bitmapUri = bitmapUri;
        this.bitmapRect = bitmapRect;
    }

    @Override
    public void init(@NonNull float[] mvpMatrix, int mvpMatrixOffset) {
        Matrix.setIdentityM(stMatrix, 0);
        // flip the bitmap vertically
        Matrix.scaleM(stMatrix, 0, 1, -1, 1);

        this.mvpMatrix = mvpMatrix;
        this.mvpMatrixOffset = mvpMatrixOffset;
        initBitmapMatrix();

        Bitmap bitmap = decodeBitmap(bitmapUri);
        if (bitmap != null) {
            createOverlayTexture(bitmap);
            bitmap.recycle();
        }
    }

    @Override
    public void apply(long presentationTimeNs) {
        if (overlayTextureID < 0) {
            return;
        }

        // Switch to overlay texture
        GLES20.glUseProgram(glOverlayProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTextureID);

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
    private void createOverlayTexture(@NonNull Bitmap overlayBitmap) {
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
    }

    @Nullable
    private Bitmap decodeBitmap(@NonNull Uri imageUri) {
        Bitmap bitmap = null;

        if (ContentResolver.SCHEME_FILE.equals(imageUri.getScheme())) {
            File file = new File(imageUri.getPath());
            bitmap = BitmapFactory.decodeFile(file.getPath());

        } else if (ContentResolver.SCHEME_CONTENT.equals(imageUri.getScheme())) {
            InputStream inputStream;
            try {
                inputStream = context.getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    bitmap = BitmapFactory.decodeStream(inputStream, null, null);
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Unable to open overlay image Uri " + imageUri, e);
            }

        } else {
            Log.e(TAG, "Uri scheme is not supported: " + imageUri.getScheme());
        }

        return bitmap;
    }

    private void initBitmapMatrix() {
        if (bitmapRect == null) {
            // just apply video frame's MVP matrix, which will fit the bitmap to entire video frame
            return;
        }

        float scaleX = bitmapRect.right - bitmapRect.left;
        float scaleY = bitmapRect.bottom - bitmapRect.top;
        float translateX = (bitmapRect.left * 2 + scaleX - 1) / scaleX;
        float translateY = (1 - bitmapRect.top * 2 - scaleY) / scaleY;
        Matrix.scaleM(mvpMatrix, mvpMatrixOffset, scaleX, scaleY, 1);
        Matrix.translateM(mvpMatrix, mvpMatrixOffset, translateX, translateY, 0);
    }
}
