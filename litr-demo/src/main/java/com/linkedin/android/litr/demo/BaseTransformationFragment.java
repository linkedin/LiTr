/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.content.Intent;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.linkedin.android.litr.demo.data.AudioTrackFormat;
import com.linkedin.android.litr.demo.data.GenericTrackFormat;
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.demo.data.TrimConfig;
import com.linkedin.android.litr.demo.data.VideoTrackFormat;
import com.linkedin.android.litr.utils.MediaFormatUtils;
import com.linkedin.android.litr.utils.TranscoderUtils;

import java.io.IOException;
import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

public class BaseTransformationFragment extends Fragment {

    private static final String TAG = BaseTransformationFragment.class.getSimpleName();

    private static final int PICK_MEDIA = 42;

    private static final String KEY_ROTATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                               ? MediaFormat.KEY_ROTATION
                                               : "rotation-degrees";

    private MediaPickerListener mediaPickerListener;

    public void pickVideo(@Nullable MediaPickerListener mediaPickerListener) {
        pickMedia("video/*", mediaPickerListener);
    }

    public void pickAudio(@Nullable MediaPickerListener mediaPickerListener) {
        pickMedia("audio/*", mediaPickerListener);
    }

    public void pickOverlay(@Nullable MediaPickerListener mediaPickerListener) {
        pickMedia("image/*", mediaPickerListener);
    }

    public void pickBackground(@Nullable MediaPickerListener mediaPickerListener) {
        pickMedia("image/*", mediaPickerListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data.getData() != null) {
            mediaPickerListener.onMediaPicked(data.getData());
        }
    }

    @NonNull
    protected void updateSourceMedia(@NonNull SourceMedia sourceMedia, @NonNull Uri uri) {
        sourceMedia.uri = uri;
        sourceMedia.size = TranscoderUtils.getSize(getContext(), uri);
        sourceMedia.duration = getMediaDuration(uri) / 1000f;

        try {
            MediaExtractor mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(getContext(), uri, null);
            sourceMedia.tracks = new ArrayList<>(mediaExtractor.getTrackCount());

            for (int track = 0; track < mediaExtractor.getTrackCount(); track++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(track);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType == null) {
                    continue;
                }

                if (mimeType.startsWith("video")) {
                    VideoTrackFormat videoTrack = new VideoTrackFormat(track, mimeType);
                    videoTrack.width = getInt(mediaFormat, MediaFormat.KEY_WIDTH);
                    videoTrack.height = getInt(mediaFormat, MediaFormat.KEY_HEIGHT);
                    videoTrack.duration = getLong(mediaFormat, MediaFormat.KEY_DURATION);
                    videoTrack.frameRate = MediaFormatUtils.getFrameRate(mediaFormat, -1).intValue();
                    videoTrack.keyFrameInterval = MediaFormatUtils.getIFrameInterval(mediaFormat, -1).intValue();
                    videoTrack.rotation = getInt(mediaFormat, KEY_ROTATION, 0);
                    videoTrack.bitrate = getInt(mediaFormat, MediaFormat.KEY_BIT_RATE);
                    sourceMedia.tracks.add(videoTrack);
                } else if (mimeType.startsWith("audio")) {
                    AudioTrackFormat audioTrack = new AudioTrackFormat(track, mimeType);
                    audioTrack.channelCount = getInt(mediaFormat, MediaFormat.KEY_CHANNEL_COUNT);
                    audioTrack.samplingRate = getInt(mediaFormat, MediaFormat.KEY_SAMPLE_RATE);
                    audioTrack.duration = getLong(mediaFormat, MediaFormat.KEY_DURATION);
                    audioTrack.bitrate = getInt(mediaFormat, MediaFormat.KEY_BIT_RATE);
                    sourceMedia.tracks.add(audioTrack);
                } else {
                    sourceMedia.tracks.add(new GenericTrackFormat(track, mimeType));
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to extract sourceMedia", ex);
        }

        sourceMedia.notifyChange();
    }

    protected void updateTrimConfig(@NonNull TrimConfig trimConfig, @NonNull SourceMedia sourceMedia) {
        trimConfig.setTrimEnd(sourceMedia.duration);
    }

    private int getInt(@NonNull MediaFormat mediaFormat, @NonNull String key) {
        return getInt(mediaFormat, key, -1);
    }

    private int getInt(@NonNull MediaFormat mediaFormat, @NonNull String key, int defaultValue) {
        if (mediaFormat.containsKey(key)) {
            return mediaFormat.getInteger(key);
        }
        return defaultValue;
    }

    private long getLong(@NonNull MediaFormat mediaFormat, @NonNull String key) {
        if (mediaFormat.containsKey(key)) {
            return mediaFormat.getLong(key);
        }
        return -1;
    }

    private void pickMedia(@NonNull String type, @Nullable MediaPickerListener mediaPickerListener) {
        this.mediaPickerListener = mediaPickerListener;

        Intent intent = new Intent();
        intent.setType(type);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_media)),
                               PICK_MEDIA);
    }

    private long getMediaDuration(@NonNull Uri uri) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(getContext(), uri);
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Long.parseLong(durationStr);
    }

}
