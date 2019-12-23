/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.utils.TransformationUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TransformationPresenter {

    private static final String TAG = TransformationPresenter.class.getSimpleName();

    private final Context context;
    private final MediaTransformer mediaTransformer;

    public TransformationPresenter(@NonNull Context context,
                                   @NonNull MediaTransformer mediaTransformer) {
        this.context = context;
        this.mediaTransformer = mediaTransformer;
    }

    public void startTransformation(@NonNull SourceMedia sourceMedia,
                                    @Nullable VideoTarget videoTarget,
                                    @Nullable AudioTarget audioTarget,
                                    @Nullable OverlayTarget overlayTarget,
                                    @NonNull TransformationState transformationState) {
        File targetVideoFile = new File(TransformationUtil.getTargetFileDirectory(),
                                   "transcoded_" + TransformationUtil.getDisplayName(context, sourceMedia.uri));
        if (targetVideoFile.exists()) {
            targetVideoFile.delete();
        }

        transformationState.targetFile = targetVideoFile;

        MediaFormat targetVideoFormat = null;
        if (videoTarget != null && videoTarget.shouldTranscodeVideo) {
            targetVideoFormat = new MediaFormat();
            targetVideoFormat.setString(MediaFormat.KEY_MIME, "video/avc");
            targetVideoFormat.setInteger(MediaFormat.KEY_WIDTH, Integer.parseInt(videoTarget.targetWidth));
            targetVideoFormat.setInteger(MediaFormat.KEY_HEIGHT, Integer.parseInt(videoTarget.targetHeight));
            targetVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, (int) (Float.parseFloat(videoTarget.targetBitrate) * 1000000));
            targetVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Integer.parseInt(videoTarget.targetKeyFrameInterval));
            targetVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        }

        MediaFormat targetAudioFormat = null;
        if (audioTarget != null && audioTarget.shouldTranscodeAudio) {
            targetAudioFormat = new MediaFormat();
            targetAudioFormat.setString(MediaFormat.KEY_MIME, sourceMedia.audioMimeType);
            targetAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT,sourceMedia.audioChannelCount);
            targetAudioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sourceMedia.audioSamplingRate);
            targetAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, Integer.parseInt(audioTarget.targetBitrate) * 1000);
        }

        List<GlFilter> glFilters = null;
        if (overlayTarget != null && overlayTarget.shouldApplyOverlay) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(overlayTarget.uri));
                if (bitmap != null) {
                    float overlayWidth = 0.56f;
                    float overlayHeight;
                    if (sourceMedia.videoRotation == 90 || sourceMedia.videoRotation == 270) {
                        float overlayWidthPixels = overlayWidth * sourceMedia.videoHeight;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / sourceMedia.videoWidth;
                    } else {
                        float overlayWidthPixels = overlayWidth * sourceMedia.videoWidth;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / sourceMedia.videoHeight;
                    }

                    PointF position = new PointF(0.6f, 0.4f);
                    PointF size = new PointF(overlayWidth, overlayHeight);
                    float rotation = 30;

                    GlFilter filter = TransformationUtil.createGlFilter(context,
                                                                        overlayTarget.uri,
                                                                        size,
                                                                        position,
                                                                        rotation);
                    if (filter != null) {
                        glFilters = Collections.singletonList(filter);
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG, "Failed to extract audio track metadata: " + ex);
            }
        }

        transformationState.requestId = UUID.randomUUID().toString();
        MediaTransformationListener transformationListener = new MediaTransformationListener(context,
                                                                                             transformationState.requestId,
                                                                                             transformationState);

        mediaTransformer.transform(transformationState.requestId,
                                   sourceMedia.uri,
                                   targetVideoFile.getAbsolutePath(),
                                   targetVideoFormat,
                                   targetAudioFormat,
                                   transformationListener,
                                   MediaTransformer.GRANULARITY_DEFAULT,
                                   glFilters);
    }

    public void cancelTransformation(@NonNull String requestId) {
        mediaTransformer.cancel(requestId);
    }

    public void play(@Nullable File targetFile) {
        if (targetFile != null && targetFile.exists()) {
            Intent playIntent = new Intent(Intent.ACTION_VIEW);
            Uri videoUri = FileProvider.getUriForFile(context,
                                                      context.getPackageName() + ".provider",
                                                      targetFile);
            playIntent.setDataAndType(videoUri, "video/*");
            playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(playIntent);
        }
    }
}
