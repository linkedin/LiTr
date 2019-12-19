/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.filter.GlFilter;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TranscodeVideoGlFragment extends BaseDemoFragment {

    private static final String TAG = TranscodeVideoGlFragment.class.getSimpleName();

    @Override
    protected void transform(@NonNull Uri sourceVideoUri,
                             @Nullable Uri overlayUri,
                             @NonNull File targetVideoFile,
                             @Nullable MediaFormat targetVideoFormat,
                             @Nullable MediaFormat targetAudioFormat) {
        targetVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        int width = targetVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = targetVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);

        String requestId = UUID.randomUUID().toString();

        List<GlFilter> glFilters = null;
        if (overlayUri != null) {
            try {
                Context context = getContext();
                Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(overlayUri));
                if (bitmap != null) {
                    int videoRotation = getVideoRotation();

                    float overlayWidth = 0.56f;
                    float overlayHeight;
                    if (videoRotation == 90 || videoRotation == 270) {
                        float overlayWidthPixels = overlayWidth * height;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / width;
                    } else {
                        float overlayWidthPixels = overlayWidth * width;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / height;
                    }

                    PointF position = new PointF(0.6f, 0.4f);
                    PointF size = new PointF(overlayWidth, overlayHeight);
                    float rotation = 30;

                    GlFilter filter = createGlFilter(overlayUri, size, position, rotation);
                    if (filter != null) {
                        glFilters = Collections.singletonList(filter);
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG, "Failed to extract audio track metadata: " + ex);
            }
        }

        mediaTransformer.transform(requestId,
                                   sourceVideoUri,
                                   targetVideoFile.getAbsolutePath(),
                                   targetVideoFormat,
                                   targetAudioFormat,
                                   videoTransformationListener,
                                   MediaTransformer.GRANULARITY_DEFAULT,
                                   glFilters);

    }
}
