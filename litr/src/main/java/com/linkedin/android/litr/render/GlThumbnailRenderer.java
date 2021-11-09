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

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.media.ThumbnailUtils;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.filter.GlFrameRenderFilter;
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A renderer that uses OpenGL to draw (and transform) decoder's output frame onto encoder's input frame. Both decoder
 * and encoder are expected to be using {@link Surface}.
 */
public class GlThumbnailRenderer {

    private final boolean hasFilters;

    private VideoRenderInputSurface inputSurface;
    private VideoRenderOutputSurface outputSurface;
    private List<GlFilter> filters;

    private float[] stMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    private boolean inputSurfaceTextureInitialized;
    private GlFramebuffer captureFBO;
    private GlTexture offscreenTexture;
    private Point targetSurfaceSize;
    private Point outputSize;
    private ByteBuffer pixelBuffer;
    private Bitmap targetSurfaceBitmap;

    public Bitmap saveTexture(int width, int height) {
        int capacity = width * height * 4;

        if (pixelBuffer == null || pixelBuffer.capacity() != capacity) {
            pixelBuffer = ByteBuffer.allocate(capacity);
        } else {
            pixelBuffer.rewind();
        }
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);
        targetSurfaceBitmap.copyPixelsFromBuffer(pixelBuffer);
        return targetSurfaceBitmap;
    }

    /**
     * Create an instance of GlVideoRenderer. If filter list has a {@link GlFrameRenderFilter}, that filter
     * will be used to render video frames. Otherwise, default {@link DefaultVideoFrameRenderFilter}
     * will be used at lowest Z level to render video frames.
     *
     * @param filters optional list of OpenGL filters to applied to output video frames
     */
    public GlThumbnailRenderer(@Nullable List<GlFilter> filters) {
        this.filters = new ArrayList<>();

        Matrix.setIdentityM(stMatrix, 0);

        hasFilters = filters != null && !filters.isEmpty();

        // TODO: Actually support other filters (for later)
        if (filters == null) {
            this.filters.add(new DefaultVideoFrameRenderFilter());
            // new Transform(new PointF(1f, 1f), new PointF(0.5f, 0.5f), 90)
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

    public void init(int sourceWidth, int sourceHeight, int destWidth, int destHeight, int sourceRotation) {
        outputSize = new Point(destWidth, destHeight);
        targetSurfaceBitmap = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);

        targetSurfaceSize = (sourceRotation == 90 || sourceRotation == 270) ?
                new Point(sourceHeight, sourceWidth) :
                new Point(sourceWidth, sourceHeight);

        inputSurface = new VideoRenderInputSurface();

        GlTexture newTexture = new GlTexture();
        SurfaceTexture surfaceTexture = new SurfaceTexture(newTexture.getTexName());
        surfaceTexture.setDefaultBufferSize(targetSurfaceSize.x, targetSurfaceSize.y);
        Surface surface = new Surface(surfaceTexture);

        // Needed to set up EGL
        this.outputSurface = new VideoRenderOutputSurface(surface);
        surfaceTexture.release();

        Matrix.setIdentityM(mvpMatrix, 0);
        // Flips the geometry on the Y axis, to produce correct orientation in the bitmap
        Matrix.scaleM(mvpMatrix, 0, 1f, -1f, 1f);

        for (GlFilter filter : filters) {
            filter.init();
            filter.setVpMatrix(mvpMatrix, 0);
        }

        offscreenTexture = new GlTexture(GLES20.GL_TEXTURE0, GLES20.GL_TEXTURE_2D, null, targetSurfaceSize.x, targetSurfaceSize.y);
        offscreenTexture.bind();
        captureFBO = new GlFramebuffer();
        captureFBO.bind();
        captureFBO.attachTexture(offscreenTexture.getTexName());
        captureFBO.unbind();
        offscreenTexture.unbind();
    }

    @Nullable
    public Surface getInputSurface() {
        if (inputSurface != null) {
            return inputSurface.getSurface();
        }
        return null;
    }

    @Nullable
    public Bitmap renderFrame(long presentationTimeNs) {
        inputSurface.awaitNewImage();
        captureFBO.bind();

        drawFrame(presentationTimeNs);
        Bitmap fullSizeBitmap = saveTexture(targetSurfaceSize.x, targetSurfaceSize.y);
        Bitmap bitmap = ThumbnailUtils.extractThumbnail(fullSizeBitmap, outputSize.x, outputSize.y);

        captureFBO.unbind();

        return bitmap;
    }


    public void release() {
        for (GlFilter filter : filters) {
            filter.release();
        }

        targetSurfaceBitmap.recycle();

        inputSurface.release();
        outputSurface.release();
        captureFBO.delete();
    }

    public boolean hasFilters() {
        return hasFilters;
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    private void drawFrame(long presentationTimeNs) {

        for (GlFilter filter : filters) {
            if (filter instanceof GlFrameRenderFilter) {
                inputSurface.getTransformMatrix(stMatrix);
                ((GlFrameRenderFilter) filter).initInputFrameTexture(inputSurface.getTextureId(), stMatrix);
            }
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        for (GlFilter filter : filters) {
            filter.apply(presentationTimeNs);
        }

        GLES20.glFinish();
    }

}
