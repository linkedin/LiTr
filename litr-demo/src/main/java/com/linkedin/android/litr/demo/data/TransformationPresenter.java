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
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.MimeType;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.utils.TransformationUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TransformationPresenter {

    private static final String TAG = TransformationPresenter.class.getSimpleName();

    protected static final String KEY_ROTATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? MediaFormat.KEY_ROTATION
            : "rotation-degrees";

    private final Context context;
    private final MediaTransformer mediaTransformer;

    public TransformationPresenter(@NonNull Context context,
                                   @NonNull MediaTransformer mediaTransformer) {
        this.context = context;
        this.mediaTransformer = mediaTransformer;
    }

    public void cancelTransformation(@NonNull String requestId) {
        mediaTransformer.cancel(requestId);
    }

    public void play(@Nullable Uri contentUri) {
        if (contentUri != null) {
            Intent playIntent = new Intent(Intent.ACTION_VIEW);
            playIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
            playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(playIntent);
        }
    }

    @Nullable
    protected List<GlFilter> createGlFilters(@NonNull SourceMedia sourceMedia,
                                           @Nullable TargetVideoTrack targetTrack,
                                           float overlayWidth,
                                           @NonNull PointF position,
                                           float rotation) {
        List<GlFilter> glFilters = null;
        if (targetTrack != null && targetTrack.overlay != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(targetTrack.overlay));
                if (bitmap != null) {
                    float overlayHeight;
                    VideoTrackFormat sourceVideoTrackFormat = (VideoTrackFormat) sourceMedia.tracks.get(targetTrack.sourceTrackIndex);
                    if (sourceVideoTrackFormat.rotation == 90 || sourceVideoTrackFormat.rotation == 270) {
                        float overlayWidthPixels = overlayWidth * sourceVideoTrackFormat.height;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / sourceVideoTrackFormat.width;
                    } else {
                        float overlayWidthPixels = overlayWidth * sourceVideoTrackFormat.width;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / sourceVideoTrackFormat.height;
                    }

                    PointF size = new PointF(overlayWidth, overlayHeight);

                    GlFilter filter = TransformationUtil.createGlFilter(context,
                                                                        targetTrack.overlay,
                                                                        size,
                                                                        position,
                                                                        rotation);
                    if (filter != null) {
                        glFilters = new ArrayList<>();
                        glFilters.add(filter);
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG, "Failed to extract audio track metadata: " + ex);
            }
        }

        return glFilters;
    }

    @Nullable
    protected MediaFormat createMediaFormat(@Nullable TargetTrack targetTrack) {
        MediaFormat mediaFormat = null;
        if (targetTrack != null && targetTrack.format != null) {
            if (targetTrack.format.mimeType.startsWith("video")) {
                mediaFormat = createVideoMediaFormat((VideoTrackFormat) targetTrack.format);
            } else if (targetTrack.format.mimeType.startsWith("audio")) {
                mediaFormat = createAudioMediaFormat((AudioTrackFormat) targetTrack.format);
            }
        }

        return mediaFormat;
    }

    @NonNull
    protected MediaFormat createVideoMediaFormat(@NonNull VideoTrackFormat trackFormat) {
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, trackFormat.mimeType);
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, trackFormat.width);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, trackFormat.height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, trackFormat.keyFrameInterval);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, trackFormat.frameRate);
        mediaFormat.setLong(MediaFormat.KEY_DURATION, trackFormat.duration);
        mediaFormat.setInteger(KEY_ROTATION, trackFormat.rotation);

        return mediaFormat;
    }

    @NonNull
    protected MediaFormat createAudioMediaFormat(@NonNull AudioTrackFormat trackFormat) {
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, trackFormat.mimeType);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, trackFormat.channelCount);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, trackFormat.samplingRate);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
        mediaFormat.setLong(MediaFormat.KEY_DURATION, trackFormat.duration);

        return mediaFormat;
    }

    protected boolean hasVp8OrVp9Track(@NonNull List<TargetTrack> targetTracks) {
        for (TargetTrack targetTrack : targetTracks) {
            if (!targetTrack.shouldInclude) {
                continue;
            }

            if (TextUtils.equals(targetTrack.format.mimeType, MimeType.VIDEO_VP8)
                    || TextUtils.equals(targetTrack.format.mimeType, MimeType.VIDEO_VP9)) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasVp8OrVp9TrackFormat(@NonNull List<MediaTrackFormat> trackFormats) {
        for (MediaTrackFormat trackFormat : trackFormats) {
            if (TextUtils.equals(trackFormat.mimeType, MimeType.VIDEO_VP8)
                    || TextUtils.equals(trackFormat.mimeType, MimeType.VIDEO_VP9)) {
                return true;
            }
        }
        return false;
    }
}
