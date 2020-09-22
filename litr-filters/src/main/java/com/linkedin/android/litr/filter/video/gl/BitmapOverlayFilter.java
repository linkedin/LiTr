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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.net.Uri;
import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.Transform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * An OpenGL filter that overlays a static bitmap on top of all video frames.
 */
public class BitmapOverlayFilter extends BaseOverlayGlFilter {

    private static final String TAG = BitmapOverlayFilter.class.getSimpleName();

    private Context context;
    private Uri bitmapUri;

    private Bitmap bitmap;

    private int overlayTextureID = -12346;
    /**
     * Create filter with bitmap URI, scale and position the bitmap into specified rectangle, with no rotation.
     * @param context context for accessing bitmap
     * @param bitmapUri bitmap {@link Uri}
     * @param bitmapRect Rectangle of bitmap's target position on a video frame, in relative coordinate in 0 - 1 range
     *                   in fourth quadrant (0,0 is top left corner)
     */
    public BitmapOverlayFilter(@NonNull Context context, @NonNull Uri bitmapUri, @Nullable RectF bitmapRect) {
        super(bitmapRect);
        this.context = context;
        this.bitmapUri = bitmapUri;
    }

    /**
     * Create filter with bitmap URI, then scale, then position and then rotate the bitmap around its center as specified.
     * @param context context for accessing bitmap
     * @param bitmapUri bitmap {@link Uri}
     * @param transform {@link Transform} that defines bitmap positioning within target video frame
     */
    public BitmapOverlayFilter(@NonNull Context context, @NonNull Uri bitmapUri, @NonNull Transform transform) {
        super(transform);
        this.context = context;
        this.bitmapUri = bitmapUri;
    }

    /**
     * Create filter with client managed {@link Bitmap}, then scale, then position and then rotate the bitmap around its center as specified.
     * @param bitmap client managed bitmap
     * @param transform {@link Transform} that defines bitmap positioning within target video frame
     */
    public BitmapOverlayFilter(@NonNull Bitmap bitmap, @Nullable Transform transform) {
        super(transform);
        this.bitmap = bitmap;
    }

    @Override
    public void init() {
        super.init();

        if (bitmap != null) {
            overlayTextureID = createOverlayTexture(bitmap);
        } else {
            Bitmap bitmap = decodeBitmap(bitmapUri);
            if (bitmap != null) {
                overlayTextureID = createOverlayTexture(bitmap);
                bitmap.recycle();
            }
        }
    }

    @Override
    public void apply(long presentationTimeNs) {
        if (overlayTextureID >= 0) {
            renderOverlayTexture(overlayTextureID);
        }
    }

    @Override
    public void release() {
        super.release();

        GLES20.glDeleteTextures(1, new int[]{overlayTextureID}, 0);
        overlayTextureID = 0;
    }

    @Nullable
    private Bitmap decodeBitmap(@NonNull Uri imageUri) {
        Bitmap bitmap = null;

        if (ContentResolver.SCHEME_FILE.equals(imageUri.getScheme()) && imageUri.getPath() != null) {
            File file = new File(imageUri.getPath());
            bitmap = BitmapFactory.decodeFile(file.getPath());

        } else if (ContentResolver.SCHEME_CONTENT.equals(imageUri.getScheme())) {
            InputStream inputStream;
            try {
                inputStream = context.getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    bitmap = BitmapFactory.decodeStream(inputStream, null, null);
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Unable to open overlay image Uri " + imageUri, e);
            }

        } else {
            Log.e(TAG, "Uri scheme is not supported: " + imageUri.getScheme());
        }

        return bitmap;
    }

}
