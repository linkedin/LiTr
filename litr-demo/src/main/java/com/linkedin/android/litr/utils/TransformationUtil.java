/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.AnimationFrameProvider;
import com.linkedin.android.litr.filter.video.gl.BitmapOverlayFilter;
import com.linkedin.android.litr.filter.video.gl.FrameSequenceAnimationOverlayFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class TransformationUtil {

    private static final String TAG = TransformationUtil.class.getSimpleName();

    private TransformationUtil() {}

    @Nullable
    public static GlFilter createGlFilter(@NonNull Context context,
                                          @NonNull Uri overlayUri,
                                          @NonNull PointF size,
                                          @NonNull PointF position,
                                          float rotation) {
        GlFilter filter = null;

        Transform transform = new Transform(size, position, rotation);

        try {
            if (TextUtils.equals(context.getContentResolver().getType(overlayUri), "image/gif")) {
                ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
                InputStream inputStream = contentResolver.openInputStream(overlayUri);
                BitmapPool bitmapPool = new LruBitmapPool(10);
                GifBitmapProvider gifBitmapProvider = new GifBitmapProvider(bitmapPool);
                final GifDecoder gifDecoder = new StandardGifDecoder(gifBitmapProvider);
                gifDecoder.read(inputStream, (int) TranscoderUtils.getSize(context, overlayUri));

                AnimationFrameProvider animationFrameProvider = new AnimationFrameProvider() {
                    @Override
                    public int getFrameCount() {
                        return gifDecoder.getFrameCount();
                    }

                    @Nullable
                    @Override
                    public Bitmap getNextFrame() {
                        return gifDecoder.getNextFrame();
                    }

                    @Override
                    public long getNextFrameDurationNs() {
                        return TimeUnit.MILLISECONDS.toNanos(gifDecoder.getNextDelay());
                    }

                    @Override
                    public void advance() {
                        gifDecoder.advance();
                    }
                };
                filter = new FrameSequenceAnimationOverlayFilter(animationFrameProvider, transform);
            } else {
                filter = new BitmapOverlayFilter(context.getApplicationContext(), overlayUri, transform);
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to create a GlFilter", ex);
        }

        return filter;
    }

    @NonNull
    public static File getTargetFileDirectory(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getNoBackupFilesDir();
        } else {
            return context.getFilesDir();
        }
    }

    @NonNull
    public static String getDisplayName(@NonNull Context context, @NonNull Uri uri) {
        String name = Long.toString(SystemClock.elapsedRealtime());

        String[] projection = { MediaStore.Video.Media.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
            cursor.close();
        }

        return name;
    }
}
