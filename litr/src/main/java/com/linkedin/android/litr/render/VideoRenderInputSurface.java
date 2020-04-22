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
// from: https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/OutputSurface.java
// blob: fc8ad9cd390c5c311f015d3b7c1359e4d295bc52
// modified: change TIMEOUT_MS from 500 to 10000
// modified: removed unused methods
package com.linkedin.android.litr.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.Surface;
import androidx.annotation.NonNull;

/**
 * Holds state associated with a Surface used for MediaCodec decoder output, and is used as a renderer input.
 * <p>
 * The (width,height) constructor for this class will prepare GL, create a SurfaceTexture,
 * and then create a Surface for that SurfaceTexture.  The Surface can be passed to
 * MediaCodec.configure() to receive decoder output.  When a frame arrives, we latch the
 * texture with updateTexImage, then render the texture with GL to a pbuffer.
 * <p>
 * The no-arg constructor skips the GL preparation step and doesn't allocate a pbuffer.
 * Instead, it just creates the Surface and SurfaceTexture, and when a frame arrives
 * we just draw it on whatever surface is current.
 * <p>
 * By default, the Surface will be using a BufferQueue in asynchronous mode, so we
 * can potentially drop frames.
 */
class VideoRenderInputSurface implements SurfaceTexture.OnFrameAvailableListener {

    private static final int FRAME_WAIT_TIMEOUT_MS = 10000;

    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private int textureId;

    private final Object frameSyncObject = new Object();
    private boolean frameAvailable;

    /**
     * Creates an RenderInputSurface using the current EGL context (rather than establishing a
     * new one).  Creates a Surface that can be passed to MediaCodec.configure().
     */
    VideoRenderInputSurface() {
        textureId = createTexture();
        surfaceTexture = new SurfaceTexture(textureId);
        surface = new Surface(surfaceTexture);
        surfaceTexture.setOnFrameAvailableListener(this);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (frameSyncObject) {
            if (frameAvailable) {
                throw new RuntimeException("frameAvailable already set, frame could be dropped");
            }
            frameAvailable = true;
            frameSyncObject.notifyAll();
        }
    }

    /**
     * Returns underlying {@link Surface}
     * @return surface
     */
    @NonNull
    Surface getSurface() {
        return surface;
    }

    /**
     * Returns texture id for input surace
     * @return texture id
     */
    int getTextureId() {
        return textureId;
    }

    @NonNull
    float[] getTransformMatrix() {
        float[] transformMatrix = new float[16];
        surfaceTexture.getTransformMatrix(transformMatrix);
        return transformMatrix;
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the OutputSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    void awaitNewImage() {
        synchronized (frameSyncObject) {
            while (!frameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    frameSyncObject.wait(FRAME_WAIT_TIMEOUT_MS);
                    if (!frameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            frameAvailable = false;
        }
        // Latch the data.
        GlRenderUtils.checkGlError("before updateTexImage");
        surfaceTexture.updateTexImage();
    }


    /**
     * Discard all resources held by this class.
     */
    void release() {
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports pbuffer.
     */
    private int createTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        int textureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);
        GlRenderUtils.checkGlError("glBindTexture textureID");
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GlRenderUtils.checkGlError("glTexParameter");

        return textureID;
    }

}
