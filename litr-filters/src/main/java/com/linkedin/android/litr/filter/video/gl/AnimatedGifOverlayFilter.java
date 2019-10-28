/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * An OpenGL filter that overlays an animated GIF on top of all video frames.
 */
public class AnimatedGifOverlayFilter extends BaseOverlayGlFilter {

    private static final String TAG = AnimatedGifOverlayFilter.class.getSimpleName();

    private Frame currentFrame;
    private long nextFramePresentationTime;

    /**
     * Create filter with certain configuration.
     * @param context context for accessing bitmap
     * @param bitmapUri bitmap {@link Uri}
     * @param bitmapRect Rectangle of bitmap's target position on a video frame, in relative coordinate in 0 - 1 range
     *                   in fourth quadrant (0,0 is top left corner)
     */
    public AnimatedGifOverlayFilter(@NonNull Context context, @NonNull Uri bitmapUri, @Nullable RectF bitmapRect) {
        super(context, bitmapUri, bitmapRect);
    }

    @Override
    public void init(@NonNull float[] mvpMatrix, int mvpMatrixOffset) {
        super.init(mvpMatrix, mvpMatrixOffset);

        try {
            ContentResolver contentResolver = context.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(bitmapUri);
            BitmapPool bitmapPool = new LruBitmapPool(10);
            GifBitmapProvider gifBitmapProvider = new GifBitmapProvider(bitmapPool);
            GifDecoder gifDecoder = new StandardGifDecoder(gifBitmapProvider);
            gifDecoder.read(inputStream, (int) getGifSize(bitmapUri));

            int frameCount = gifDecoder.getFrameCount();
            Frame firstFrame = null;
            Frame prevFrame = null;
            Frame frame = null;
            Bitmap frameBitmap;
            for (int frameIdx = 0; frameIdx < frameCount; frameIdx++) {
                gifDecoder.advance();
                frameBitmap = gifDecoder.getNextFrame();
                if (frameBitmap == null) {
                    Log.e(TAG, "Error loading GIF frame " + frameIdx);
                    continue;
                }
                int textureId = createOverlayTexture(frameBitmap);
                frame = new Frame(textureId, gifDecoder.getNextDelay());
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
                nextFramePresentationTime = TimeUnit.MILLISECONDS.toNanos(currentFrame.delay);
            }
        } catch (IOException ex) {
            Log.e(TAG, "Error loading animated gif", ex);
        }
    }

    @Override
    public void apply(long presentationTimeNs) {
        if (currentFrame == null) {
            return;
        }

        if (presentationTimeNs > nextFramePresentationTime) {
            currentFrame = currentFrame.next;
            nextFramePresentationTime += TimeUnit.MILLISECONDS.toNanos(currentFrame.delay);
        }

        renderOverlayTexture(currentFrame.textureId);
    }

    private long getGifSize(@NonNull Uri uri) {
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            AssetFileDescriptor fileDescriptor = null;
            try {
                fileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                long size = fileDescriptor != null ? fileDescriptor.getParcelFileDescriptor().getStatSize() : 0;
                return size < 0 ? 0 : size;
            } catch (FileNotFoundException | IllegalStateException e) {
                Log.e(TAG, "Unable to extract length from uri: " + uri, e);
                return 0;
            } finally {
                if (fileDescriptor != null) {
                    try {
                        fileDescriptor.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to close file descriptor from uri: " + uri, e);
                    }
                }
            }
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme()) && uri.getPath() != null) {
            File file = new File(uri.getPath());
            return file.length();
        } else {
            return 0;
        }
    }

    private static class Frame {
        private int textureId;
        private int delay;
        private Frame next;

        private Frame(@IntRange(from = 0) int textureId, @IntRange(from = 0) int delay) {
            this.textureId = textureId;
            this.delay = delay;
        }
    }
}
