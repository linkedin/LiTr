/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.preview;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

public class VideoFilterPreviewView extends GLSurfaceView {

    public VideoFilterPreviewView(Context context) {
        this(context, null);
    }

    public VideoFilterPreviewView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        setEGLContextFactory(new PreviewEglContextFactory());
        setEGLConfigChooser(new PreviewEglConfigChooser());
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer(renderer);

        if (renderer instanceof VideoPreviewRenderer) {
            ((VideoPreviewRenderer) renderer).setPreviewRenderListener(new VideoPreviewRenderer.PreviewRenderListener() {
                @Override
                public void onRenderRequested() {
                    requestRender();
                }

                @Override
                public void onEventQueued(@NonNull Runnable runnable) {
                    queueEvent(runnable);
                }
            });
        }
    }
}
