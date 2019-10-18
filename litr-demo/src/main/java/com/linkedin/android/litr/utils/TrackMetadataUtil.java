/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.analytics.TrackTransformationInfo;
import com.linkedin.android.litr.demo.R;

import java.io.IOException;
import java.util.List;

public class TrackMetadataUtil {

    private static final String TAG = TrackMetadataUtil.class.getSimpleName();

    private static final String KEY_ROTATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                               ? MediaFormat.KEY_ROTATION
                                               : "rotation-degrees";

    @NonNull
    public static String printTrackMetadata(@NonNull Context context, @NonNull Uri uri) {
        StringBuilder trackMetadataStringBuilder = new StringBuilder();

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(context, uri);
        String rotation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (rotation != null) {
            trackMetadataStringBuilder.append(context.getString(R.string.rotation, Integer.parseInt(rotation)));
        }

        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(context, uri, null);
            int trackCount = mediaExtractor.getTrackCount();
            for (int track = 0; track < trackCount; track++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(track);
                String mimeType = null;
                if (mediaFormat.containsKey(MediaFormat.KEY_MIME)) {
                    mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                }
                trackMetadataStringBuilder.append(context.getString(R.string.track, track));
                if (mimeType != null && mimeType.startsWith("video")) {
                    trackMetadataStringBuilder.append(printVideoMetadata(context, mediaFormat));
                } else if (mimeType != null && mimeType.startsWith("audio")) {
                    trackMetadataStringBuilder.append(printAudioMetadata(context, mediaFormat));
                } else if (mimeType != null && mimeType.startsWith("image")) {
                    trackMetadataStringBuilder.append(printImageMetadata(context, mediaFormat));
                } else {
                    trackMetadataStringBuilder.append(printGenericMetadata(context, mediaFormat));
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Exception when trying to extract metadata from " + uri, ex);
        }
        return trackMetadataStringBuilder.toString();
    }

    @NonNull
    public static String printTransformationStats(@NonNull Context context,
                                                  @Nullable List<TrackTransformationInfo> stats) {
        if (stats == null || stats.isEmpty()) {
            return context.getString(R.string.no_transformation_stats);
        }

        StringBuilder statsStringBuilder = new StringBuilder();
        for (int track = 0; track < stats.size(); track++) {
            statsStringBuilder.append(context.getString(R.string.track, track));

            TrackTransformationInfo trackTransformationInfo = stats.get(track);
            MediaFormat sourceFormat = trackTransformationInfo.getSourceFormat();
            String mimeType = null;
            if (sourceFormat.containsKey(MediaFormat.KEY_MIME)) {
                mimeType = sourceFormat.getString(MediaFormat.KEY_MIME);
            }
            if (mimeType != null && mimeType.startsWith("video")) {
                statsStringBuilder.append(context.getString(R.string.source_format))
                                  .append(printVideoMetadata(context, sourceFormat))
                                  .append(context.getString(R.string.target_format))
                                  .append(printVideoMetadata(context, trackTransformationInfo.getTargetFormat()));
            } else if (mimeType != null && mimeType.startsWith("audio")) {
                statsStringBuilder.append(context.getString(R.string.source_format))
                                  .append(printAudioMetadata(context, sourceFormat))
                                  .append(context.getString(R.string.target_format))
                                  .append(printAudioMetadata(context, trackTransformationInfo.getTargetFormat()));
            } else if (mimeType != null && mimeType.startsWith("image")) {
                statsStringBuilder.append(context.getString(R.string.source_format))
                                  .append(printImageMetadata(context, sourceFormat))
                                  .append(context.getString(R.string.target_format))
                                  .append(printImageMetadata(context, trackTransformationInfo.getTargetFormat()));
            } else {
                statsStringBuilder.append(context.getString(R.string.source_format))
                                  .append(printGenericMetadata(context, sourceFormat))
                                  .append(context.getString(R.string.target_format))
                                  .append(printGenericMetadata(context, trackTransformationInfo.getTargetFormat()));
            }
            statsStringBuilder.append(context.getString(R.string.decoder, trackTransformationInfo.getDecoderCodec()))
                              .append(context.getString(R.string.encoder, trackTransformationInfo.getEncoderCodec()))
                              .append(context.getString(R.string.transformation_duration, trackTransformationInfo.getDuration()))
                              .append('\n');
        }
        return statsStringBuilder.toString();
    }

    @NonNull
    private static String printVideoMetadata(@NonNull Context context, @Nullable MediaFormat mediaFormat) {
        if (mediaFormat == null) {
            return "\n";
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (mediaFormat.containsKey(MediaFormat.KEY_MIME)) {
            stringBuilder.append(context.getString(R.string.mime_type, mediaFormat.getString(MediaFormat.KEY_MIME)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_WIDTH)) {
            stringBuilder.append(context.getString(R.string.width, mediaFormat.getInteger(MediaFormat.KEY_WIDTH)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
            stringBuilder.append(context.getString(R.string.height, mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
            stringBuilder.append(context.getString(R.string.bitrate, mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            stringBuilder.append(context.getString(R.string.duration, mediaFormat.getLong(MediaFormat.KEY_DURATION)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
            stringBuilder.append(context.getString(R.string.frame_rate, mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) {
            stringBuilder.append(context.getString(R.string.key_frame_interval, mediaFormat.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL)));
        }
        if (mediaFormat.containsKey(KEY_ROTATION)) {
            stringBuilder.append(context.getString(R.string.rotation, mediaFormat.getInteger(KEY_ROTATION)));
        }
        return stringBuilder.toString();
    }

    @NonNull
    private static String printAudioMetadata(@NonNull Context context, @Nullable MediaFormat mediaFormat) {
        if (mediaFormat == null) {
            return "\n";
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (mediaFormat.containsKey(MediaFormat.KEY_MIME)) {
            stringBuilder.append(context.getString(R.string.mime_type, mediaFormat.getString(MediaFormat.KEY_MIME)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            stringBuilder.append(context.getString(R.string.channel_count, mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
            stringBuilder.append(context.getString(R.string.bitrate, mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            stringBuilder.append(context.getString(R.string.duration, mediaFormat.getLong(MediaFormat.KEY_DURATION)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            stringBuilder.append(context.getString(R.string.sample_rate, mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)));
        }
        return stringBuilder.toString();
    }

    @NonNull
    private static String printImageMetadata(@NonNull Context context, @Nullable MediaFormat mediaFormat) {
        if (mediaFormat == null) {
            return "\n";
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (mediaFormat.containsKey(MediaFormat.KEY_MIME)) {
            stringBuilder.append(context.getString(R.string.mime_type, mediaFormat.getString(MediaFormat.KEY_MIME)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_WIDTH)) {
            stringBuilder.append(context.getString(R.string.width, mediaFormat.getInteger(MediaFormat.KEY_WIDTH)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
            stringBuilder.append(context.getString(R.string.height, mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)));
        }
        return stringBuilder.toString();
    }

    @NonNull
    private static String printGenericMetadata(@NonNull Context context, @Nullable MediaFormat mediaFormat) {
        if (mediaFormat == null) {
            return "\n";
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_MIME)) {
            return context.getString(R.string.mime_type, mediaFormat.getString(MediaFormat.KEY_MIME));
        }
        return "\n";
    }
}
