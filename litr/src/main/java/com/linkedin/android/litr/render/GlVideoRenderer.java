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

import android.media.MediaFormat;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.linkedin.android.litr.codec.Frame;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.filter.GlFrameRenderFilter;
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A renderer that uses OpenGL to draw (and transform) decoder's output frame onto encoder's input frame. Both decoder
 * and encoder are expected to be using {@link Surface}.
 */
public class GlVideoRenderer implements Renderer {

    protected static final String KEY_ROTATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                                 ? MediaFormat.KEY_ROTATION
                                                 : "rotation-degrees";

    private final boolean hasFilters;

    private VideoRenderInputSurface inputSurface;
    private VideoRenderOutputSurface outputSurface;
    private List<GlFilter> filters;

    private float[] mvpMatrix = new float[16];

    private boolean inputSurfaceTextureInitialized;

    /**
     * Create an instance of GlVideoRenderer. If filter list has a {@link GlFrameRenderFilter}, that filter
     * will be used to render video frames. Otherwise, default {@link DefaultVideoFrameRenderFilter}
     * will be used at lowest Z level to render video frames.
     * @param filters optional list of OpenGL filters to applied to output video frames
     */
    public GlVideoRenderer(@Nullable List<GlFilter> filters) {
        this.filters = new ArrayList<>();
        hasFilters = filters != null && !filters.isEmpty();

        if (filters == null) {
            this.filters.add(new DefaultVideoFrameRenderFilter());
            return;
        }

        boolean hasFrameRenderFilter = false;
        for (GlFilter filter : filters) {
            if (filter instanceof GlFrameRenderFilter) {
                hasFrameRenderFilter = true;
                break;
            }
        }
        if (!hasFrameRenderFilter) {
            // if client provided filters don't have a frame render filter, insert default frame filter
            this.filters.add(new DefaultVideoFrameRenderFilter());
        }
        this.filters.addAll(filters);
    }

    @Override
    public void init(@Nullable Surface outputSurface, @Nullable MediaFormat sourceMediaFormat, @Nullable MediaFormat targetMediaFormat) {
        if (outputSurface == null) {
            throw new IllegalArgumentException("GlVideoRenderer requires an output surface");
        }
        if (targetMediaFormat == null) {
            throw new IllegalArgumentException("GlVideoRenderer requires target media format");
        }

        // prioritize target video rotation value, fall back to source video rotation value
        int rotation = 0;
        if (targetMediaFormat.containsKey(KEY_ROTATION)) {
            rotation = targetMediaFormat.getInteger(KEY_ROTATION);
        } else if (sourceMediaFormat != null && sourceMediaFormat.containsKey(KEY_ROTATION)) {
            rotation = sourceMediaFormat.getInteger(KEY_ROTATION);
        }
        float aspectRatio = 1;
        if (targetMediaFormat.containsKey(MediaFormat.KEY_WIDTH) && targetMediaFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
            aspectRatio = (float) targetMediaFormat.getInteger(MediaFormat.KEY_WIDTH) / targetMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        }

        this.outputSurface = new VideoRenderOutputSurface(outputSurface);

        inputSurface = new VideoRenderInputSurface();
        initMvpMatrix(rotation, aspectRatio);

        for (GlFilter filter : filters) {
            filter.init();
            filter.setVpMatrix(Arrays.copyOf(mvpMatrix, mvpMatrix.length), 0);
        }
    }

    @Override
    public void onMediaFormatChanged(@Nullable MediaFormat sourceMediaFormat, @Nullable MediaFormat targetMediaFormat) {}

    @Override
    @Nullable
    public Surface getInputSurface() {
        if (inputSurface != null) {
            return inputSurface.getSurface();
        }
        return null;
    }

    @Override
    public void renderFrame(@Nullable Frame inputFrame, long presentationTimeNs) {
        inputSurface.awaitNewImage();
        drawFrame(presentationTimeNs);
        outputSurface.setPresentationTime(presentationTimeNs);
        outputSurface.swapBuffers();
    }

    @Override
    public void release() {
        for (GlFilter filter : filters) {
            filter.release();
        }

        inputSurface.release();
        outputSurface.release();
    }

    @Override
    public boolean hasFilters() {
        return hasFilters;
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    private void drawFrame(long presentationTimeNs) {
        initInputSurfaceTexture();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        for (GlFilter filter : filters) {
            filter.apply(presentationTimeNs);
        }

        GLES20.glFinish();
    }

    private void initMvpMatrix(int rotation, float videoAspectRatio) {
        float[] projectionMatrix = new float[16];
        Matrix.setIdentityM(projectionMatrix, 0);
        Matrix.orthoM(projectionMatrix, 0, -videoAspectRatio, videoAspectRatio, -1, 1, -1, 1);

        // rotate the camera to match video frame rotation
        float[] viewMatrix = new float[16];
        Matrix.setIdentityM(viewMatrix, 0);
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
                upX = (float) Math.sin(rotation / 180 * Math.PI);
                upY = (float) Math.cos(rotation / 180 * Math.PI);
                break;
        }
        Matrix.setLookAtM(viewMatrix, 0,
                          0, 0, 1,
                          0, 0, 0,
                          upX, upY, 0);

        Matrix.setIdentityM(mvpMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    private void initInputSurfaceTexture() {
        if (!inputSurfaceTextureInitialized) {
            for (GlFilter filter : filters) {
                if (filter instanceof GlFrameRenderFilter) {
                    ((GlFrameRenderFilter) filter).initInputFrameTexture(inputSurface.getTextureId(), inputSurface.getTransformMatrix());
                }
            }
            inputSurfaceTextureInitialized = true;
        }
    }
}
