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
// from: https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/InputSurface.java
// blob: 157ed88d143229e4edb6889daf18fb73aa2fc5a5
// modified: removed unused methods
package com.linkedin.android.litr.render;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;
import androidx.annotation.NonNull;

/**
 * Holds state associated with a Surface used for MediaCodec encoder input.
 * <p>
 * The constructor takes a Surface obtained from MediaCodec.createInputSurface(), and uses that
 * to create an EGL window surface.  Calls to eglSwapBuffers() cause a frame of data to be sent
 * to the video encoder.
 */
class VideoRenderOutputSurface {

    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;

    private Surface surface;

    VideoRenderOutputSurface(@NonNull Surface surface) {
        this.surface = surface;
        eglSetup();
        makeCurrent();
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    boolean swapBuffers() {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface);
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    void setPresentationTime(long nanoseconds) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nanoseconds);
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    public void release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
            EGL14.eglDestroyContext(eglDisplay, eglContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(eglDisplay);

            eglDisplay = EGL14.EGL_NO_DISPLAY;
            eglContext = EGL14.EGL_NO_CONTEXT;
            eglSurface = EGL14.EGL_NO_SURFACE;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }

    private void eglSetup() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            eglDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Configure EGL for recordable and OpenGL ES 2.0.  We want enough RGB bits
        // to minimize artifacts from possible YUV conversion.
        int[] egl14ConfigAttributes = {
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(eglDisplay,
                                   egl14ConfigAttributes, 0,
                                   configs, 0,
                                   configs.length, numConfigs, 0)) {
            throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
        }

        // Configure context for OpenGL ES 2.0.
        int[] egl14ContextAttributes = {
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        };
        eglContext = EGL14.eglCreateContext(eglDisplay,
                                            configs[0],
                                            EGL14.EGL_NO_CONTEXT,
                                            egl14ContextAttributes,
                                            0);
        checkEglError("eglCreateContext");
        if (eglContext == null) {
            throw new RuntimeException("null context");
        }

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
            EGL14.EGL_NONE
        };
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay,
                                                  configs[0],
                                                  surface,
                                                  surfaceAttribs,
                                                  0);
        checkEglError("eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
    }

    /**
     * Makes our EGL context and surface current.
     */
    private void makeCurrent() {
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    private void checkEglError(@NonNull String message) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(message + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }
}
