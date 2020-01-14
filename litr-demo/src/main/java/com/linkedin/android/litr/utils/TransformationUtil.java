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
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Environment;
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
import com.linkedin.android.litr.filter.video.gl.AnimationFrameProvider;
import com.linkedin.android.litr.filter.video.gl.BitmapOverlayFilter;
import com.linkedin.android.litr.filter.video.gl.FrameSequenceAnimationOverlayFilter;

import java.io.File;
import java.io.FileNotFoundException;
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

        try {
            if (TextUtils.equals(context.getContentResolver().getType(overlayUri), "image/gif")) {
                ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
                InputStream inputStream = contentResolver.openInputStream(overlayUri);
                BitmapPool bitmapPool = new LruBitmapPool(10);
                GifBitmapProvider gifBitmapProvider = new GifBitmapProvider(bitmapPool);
                final GifDecoder gifDecoder = new StandardGifDecoder(gifBitmapProvider);
                gifDecoder.read(inputStream, (int) getSize(context, overlayUri));

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
                filter = new FrameSequenceAnimationOverlayFilter(animationFrameProvider, size, position, rotation);
            } else {
                filter = new BitmapOverlayFilter(context.getApplicationContext(), overlayUri, size, position, rotation);
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to create a GlFilter", ex);
        }

        return filter;
    }

    public static long getSize(@NonNull Context context, @NonNull Uri uri) {
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            AssetFileDescriptor fileDescriptor = null;
            try {
                fileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                long size = fileDescriptor != null ? fileDescriptor.getParcelFileDescriptor().getStatSize() : 0;
                return size < 0 ? 0 : size;
            } catch (FileNotFoundException | IllegalStateException e) {
                Log.e(TAG, "Unable to extract length from targetFile: " + uri, e);
                return 0;
            } finally {
                if (fileDescriptor != null) {
                    try {
                        fileDescriptor.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to close file descriptor from targetFile: " + uri, e);
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

    @NonNull
    public static File getTargetFileDirectory() {
        File targetDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                                            + File.separator
                                            + "LiTr");
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        return targetDirectory;
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
