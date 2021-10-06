/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils;

import android.content.Context;
import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.analytics.TrackTransformationInfo;
import com.linkedin.android.litr.demo.R;

import java.util.List;

public class TrackMetadataUtil {

    private static final String TAG = TrackMetadataUtil.class.getSimpleName();

    private static final String KEY_ROTATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                               ? MediaFormat.KEY_ROTATION
                                               : "rotation-degrees";

    @NonNull
    public static String printTransformationStats(@NonNull Context context,
                                                  @Nullable List<TrackTransformationInfo> stats) {
        if (stats == null || stats.isEmpty()) {
            return context.getString(R.string.no_transformation_stats);
        }

        StringBuilder statsStringBuilder = new StringBuilder();
        for (int track = 0; track < stats.size(); track++) {
            statsStringBuilder.append(context.getString(R.string.stats_track, track));

            TrackTransformationInfo trackTransformationInfo = stats.get(track);
            MediaFormat sourceFormat = trackTransformationInfo.getSourceFormat();
            String mimeType = null;
            if (sourceFormat.containsKey(MediaFormat.KEY_MIME)) {
                mimeType = sourceFormat.getString(MediaFormat.KEY_MIME);
            }
            if (mimeType != null && mimeType.startsWith("video")) {
                statsStringBuilder.append(context.getString(R.string.stats_source_format))
                                  .append(printVideoMetadata(context, sourceFormat))
                                  .append(context.getString(R.string.stats_target_format))
                                  .append(printVideoMetadata(context, trackTransformationInfo.getTargetFormat()));
            } else if (mimeType != null && mimeType.startsWith("audio")) {
                statsStringBuilder.append(context.getString(R.string.stats_source_format))
                                  .append(printAudioMetadata(context, sourceFormat))
                                  .append(context.getString(R.string.stats_target_format))
                                  .append(printAudioMetadata(context, trackTransformationInfo.getTargetFormat()));
            } else if (mimeType != null && mimeType.startsWith("image")) {
                statsStringBuilder.append(context.getString(R.string.stats_source_format))
                                  .append(printImageMetadata(context, sourceFormat))
                                  .append(context.getString(R.string.stats_target_format))
                                  .append(printImageMetadata(context, trackTransformationInfo.getTargetFormat()));
            } else {
                statsStringBuilder.append(context.getString(R.string.stats_source_format))
                                  .append(printGenericMetadata(context, sourceFormat))
                                  .append(context.getString(R.string.stats_target_format))
                                  .append(printGenericMetadata(context, trackTransformationInfo.getTargetFormat()));
            }
            statsStringBuilder.append(context.getString(R.string.stats_decoder, trackTransformationInfo.getDecoderCodec()))
                              .append(context.getString(R.string.stats_encoder, trackTransformationInfo.getEncoderCodec()))
                              .append(context.getString(R.string.stats_transformation_duration, trackTransformationInfo.getDuration()))
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
            stringBuilder.append(context.getString(R.string.stats_mime_type, mediaFormat.getString(MediaFormat.KEY_MIME)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_WIDTH)) {
            stringBuilder.append(context.getString(R.string.stats_width, mediaFormat.getInteger(MediaFormat.KEY_WIDTH)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
            stringBuilder.append(context.getString(R.string.stats_height, mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
            stringBuilder.append(context.getString(R.string.stats_bitrate, mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            stringBuilder.append(context.getString(R.string.stats_duration, mediaFormat.getLong(MediaFormat.KEY_DURATION)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
            stringBuilder.append(context.getString(R.string.stats_frame_rate, MediaFormatUtils.getFrameRate(mediaFormat, 0).intValue()));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) {
            stringBuilder.append(context.getString(R.string.stats_key_frame_interval, MediaFormatUtils.getIFrameInterval(mediaFormat, 0).intValue()));
        }
        if (mediaFormat.containsKey(KEY_ROTATION)) {
            stringBuilder.append(context.getString(R.string.stats_rotation, mediaFormat.getInteger(KEY_ROTATION)));
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
            stringBuilder.append(context.getString(R.string.stats_mime_type, mediaFormat.getString(MediaFormat.KEY_MIME)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            stringBuilder.append(context.getString(R.string.stats_channel_count, mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
            stringBuilder.append(context.getString(R.string.stats_bitrate, mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            stringBuilder.append(context.getString(R.string.stats_duration, mediaFormat.getLong(MediaFormat.KEY_DURATION)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            stringBuilder.append(context.getString(R.string.stats_sampling_rate, mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)));
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
            stringBuilder.append(context.getString(R.string.stats_mime_type, mediaFormat.getString(MediaFormat.KEY_MIME)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_WIDTH)) {
            stringBuilder.append(context.getString(R.string.stats_width, mediaFormat.getInteger(MediaFormat.KEY_WIDTH)));
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
            stringBuilder.append(context.getString(R.string.stats_height, mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)));
        }
        return stringBuilder.toString();
    }

    @NonNull
    private static String printGenericMetadata(@NonNull Context context, @Nullable MediaFormat mediaFormat) {
        if (mediaFormat == null) {
            return "\n";
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_MIME)) {
            return context.getString(R.string.stats_mime_type, mediaFormat.getString(MediaFormat.KEY_MIME));
        }
        return "\n";
    }
}
