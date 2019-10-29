/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An OpenGL filter that overlays a sprite animation (such as animated GIF) on top of all video frames.
 */
public class FrameSequenceAnimationOverlayFilter extends BaseOverlayGlFilter {

    private static final String TAG = FrameSequenceAnimationOverlayFilter.class.getSimpleName();

    private final AnimationFrameProvider animationFrameProvider;

    private Frame currentFrame;
    private long nextFramePresentationTime;

    /**
     * Create filter with certain configuration.
     * @param animationFrameProvider {@link AnimationFrameProvider} which provides animation frames and their durations
     * @param bitmapRect Rectangle of bitmap's target position on a video frame, in relative coordinate in 0 - 1 range
     *                   in fourth quadrant (0,0 is top left corner)
     */
    public FrameSequenceAnimationOverlayFilter(@NonNull AnimationFrameProvider animationFrameProvider, @Nullable RectF bitmapRect) {
        super(bitmapRect);

        this.animationFrameProvider = animationFrameProvider;
    }

    @Override
    public void init(@NonNull float[] mvpMatrix, int mvpMatrixOffset) {
        super.init(mvpMatrix, mvpMatrixOffset);

        Frame firstFrame = null;
        Frame prevFrame = null;
        Frame frame = null;
        Bitmap frameBitmap;
        for (int frameIdx = 0; frameIdx < animationFrameProvider.getFrameCount(); frameIdx++) {
            animationFrameProvider.advance();
            frameBitmap = animationFrameProvider.getNextFrame();
            if (frameBitmap == null) {
                Log.e(TAG, "Error loading GIF frame " + frameIdx);
                continue;
            }
            int textureId = createOverlayTexture(frameBitmap);
            frame = new Frame(textureId, animationFrameProvider.getNextFrameDurationNs());
            if (frameIdx == 0) {
                firstFrame = frame;
            }
            if (prevFrame != null) {
                prevFrame.next = frame;
            }
            prevFrame = frame;
            frameBitmap.recycle();
        }
        if (frame != null) {
            frame.next = firstFrame;
        }
        if (firstFrame != null) {
            currentFrame = firstFrame;
            nextFramePresentationTime = currentFrame.duration;
        }
    }

    @Override
    public void apply(long presentationTimeNs) {
        if (currentFrame == null) {
            return;
        }

        if (presentationTimeNs > nextFramePresentationTime) {
            currentFrame = currentFrame.next;
            nextFramePresentationTime += currentFrame.duration;
        }

        renderOverlayTexture(currentFrame.textureId);
    }

    private static class Frame {
        private int textureId;
        private long duration;
        private Frame next;

        private Frame(@IntRange(from = 0) int textureId, @IntRange(from = 0) long duration) {
            this.textureId = textureId;
            this.duration = duration;
        }
    }
}
