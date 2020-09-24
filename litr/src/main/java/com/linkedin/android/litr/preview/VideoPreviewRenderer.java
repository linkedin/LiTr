/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.preview;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.GlFrameRenderFilter;
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter;

import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_MAX_TEXTURE_SIZE;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;

/**
 * An implementation of {@link GLSurfaceView.Renderer} which renders a preview of a video with
 * {@link GlFrameRenderFilter} applied. Works in conjunction with {@link VideoFilterPreviewView}
 * Once initialization is completed, calls back {@link InputSurfaceTextureListener} with an instance of
 * a {@link SurfaceTexture} a video player (e.g. ExoPlayer) or camera preview should render onto.
 */
public class VideoPreviewRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = VideoPreviewRenderer.class.getSimpleName();

    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private final InputSurfaceTextureListener inputSurfaceTextureListener;

    private float[] stMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    private SurfaceTexture previewSurfaceTexture;
    private int textureHandle;

    private GlFrameRenderFilter frameRenderFilter;

    private PreviewRenderListener previewRenderListener;

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            previewRenderListener.onRenderRequested();
        }
    };

    public VideoPreviewRenderer(@NonNull InputSurfaceTextureListener inputSurfaceTextureListener) {
        this.inputSurfaceTextureListener = inputSurfaceTextureListener;
        this.frameRenderFilter = new DefaultVideoFrameRenderFilter();

        Matrix.setIdentityM(stMatrix, 0);
    }

    public void setFilter(@NonNull final GlFrameRenderFilter frameRenderFilter) {
        if (this.frameRenderFilter != frameRenderFilter && previewRenderListener != null) {
            previewRenderListener.onEventQueued(new Runnable() {
                @Override
                public void run() {
                    VideoPreviewRenderer.this.frameRenderFilter.release();
                    if (previewSurfaceTexture != null) {
                        frameRenderFilter.init();
                        frameRenderFilter.setVpMatrix(Arrays.copyOf(mvpMatrix, mvpMatrix.length), 0);
                    }
                    VideoPreviewRenderer.this.frameRenderFilter = frameRenderFilter;
                }
            });
        }
    }

    public void setPreviewRenderListener(@Nullable PreviewRenderListener previewRenderListener) {
        this.previewRenderListener = previewRenderListener;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        final int[] args = new int[1];

        GLES20.glGenTextures(args.length, args, 0);
        textureHandle = args[0];

        previewSurfaceTexture = new SurfaceTexture(textureHandle);
        previewSurfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);

        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureHandle);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        inputSurfaceTextureListener.onSurfaceTextureCreated(previewSurfaceTexture);

        frameRenderFilter.init();

        GLES20.glGetIntegerv(GL_MAX_TEXTURE_SIZE, args, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        initMvpMatrix((float) width / height);
        frameRenderFilter.setVpMatrix(Arrays.copyOf(mvpMatrix, mvpMatrix.length), 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            previewSurfaceTexture.updateTexImage();
            previewSurfaceTexture.getTransformMatrix(stMatrix);
        }

        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        frameRenderFilter.initInputFrameTexture(textureHandle, stMatrix);
        frameRenderFilter.apply(previewSurfaceTexture.getTimestamp());
    }

    public void release() {
        frameRenderFilter.release();

        if (previewSurfaceTexture != null) {
            previewSurfaceTexture.release();
        }
    }

    private void initMvpMatrix(float videoAspectRatio) {
        float[] projectionMatrix = new float[16];
        Matrix.setIdentityM(projectionMatrix, 0);
        Matrix.orthoM(projectionMatrix, 0, -videoAspectRatio, videoAspectRatio, -1, 1, -1, 1);

        // rotate the camera to match video frame rotation
        float[] viewMatrix = new float[16];
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.setLookAtM(viewMatrix, 0,
                0, 0, 1,
                0, 0, 0,
                0, 1, 0);

        Matrix.setIdentityM(mvpMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    /**
     * A listener which notifies when input surface is created.
     */
    public interface InputSurfaceTextureListener {

        /**
         * Input surface is created
         * @param surfaceTexture input texture surface, which video player or camera preview can render onto
         */
        void onSurfaceTextureCreated(@NonNull SurfaceTexture surfaceTexture);
    }

    /**
     * A listener used for communication with preview view
     */
    interface PreviewRenderListener {

        /**
         * Requesting preview view to render
         */
        void onRenderRequested();

        /**
         * Queue a {@link Runnable} to be run on preview surface render thread
         * @param runnable Runnable to run
         */
        void onEventQueued(@NonNull Runnable runnable);
    }
}
