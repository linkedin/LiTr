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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.utils.TransformationUtil;

import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class MediaPickerFragment extends Fragment {

    public static final int PICK_VIDEO = 0;
    public static final int PICK_OVERLAY = 1;

    public static final String KEY_PICK_TYPE = "pickType";

    private static final String TAG = MediaPickerFragment.class.getSimpleName();

    private static final String KEY_ROTATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                               ? MediaFormat.KEY_ROTATION
                                               : "rotation-degrees";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            int pickType = arguments.getInt(KEY_PICK_TYPE);
            String type = null;
            switch (pickType) {
                case PICK_VIDEO:
                    type = "video/*";
                    break;
                case PICK_OVERLAY:
                    type = "image/*";
                    break;
            }

            if (type != null) {
                Intent intent = new Intent();
                intent.setType(type);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_media)),
                                       pickType);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (getTargetFragment() instanceof MediaPickerTarget && data.getData() != null) {
                MediaPickerTarget mediaPickerTarget = (MediaPickerTarget) getTargetFragment();
                switch (requestCode) {
                    case PICK_VIDEO:
                        mediaPickerTarget.onMediaPicked(createSourceMedia(data.getData()));
                        break;
                    case PICK_OVERLAY:
                        mediaPickerTarget.onOverlayPicked(data.getData(),
                                                          TransformationUtil.getSize(getContext(), data.getData()));
                        break;
                }
            }
        }

        getActivity().getSupportFragmentManager().popBackStack();
    }

    @NonNull
    private SourceMedia createSourceMedia(@NonNull Uri uri) {
        SourceMedia sourceMedia = new SourceMedia();
        sourceMedia.uri = uri;
        sourceMedia.size = TransformationUtil.getSize(getContext(), uri);

        try {
            MediaExtractor mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(getContext(), uri, null);
            for (int track = 0; track < mediaExtractor.getTrackCount(); track++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(track);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType == null) {
                    continue;
                }

                if (mimeType.startsWith("video")) {
                    sourceMedia.videoTrack = track;
                    sourceMedia.videoMimeType = mimeType;
                    sourceMedia.videoWidth = getInt(mediaFormat, MediaFormat.KEY_WIDTH);
                    sourceMedia.videoHeight = getInt(mediaFormat, MediaFormat.KEY_HEIGHT);
                    sourceMedia.videoDuration = getLong(mediaFormat, MediaFormat.KEY_DURATION);
                    sourceMedia.videoFrameRate = getInt(mediaFormat, MediaFormat.KEY_FRAME_RATE);
                    sourceMedia.videoKeyFrameInterval = getInt(mediaFormat, MediaFormat.KEY_I_FRAME_INTERVAL);
                    sourceMedia.videoRotation = getInt(mediaFormat, KEY_ROTATION);
                    sourceMedia.videoBitrate = getInt(mediaFormat, MediaFormat.KEY_BIT_RATE);
                    if (sourceMedia.videoBitrate > 0) {
                        sourceMedia.videoBitrate /= 1000000;
                    }
                } else if (mimeType.startsWith("audio")) {
                    sourceMedia.audioTrack = track;
                    sourceMedia.audioMimeType = mimeType;
                    sourceMedia.audioChannelCount = getInt(mediaFormat, MediaFormat.KEY_CHANNEL_COUNT);
                    sourceMedia.audioSamplingRate = getInt(mediaFormat, MediaFormat.KEY_SAMPLE_RATE);
                    sourceMedia.audioDuration = getLong(mediaFormat, MediaFormat.KEY_DURATION);
                    sourceMedia.audioBitrate = getInt(mediaFormat, MediaFormat.KEY_BIT_RATE) / 1000;
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to extract sourceMedia", ex);
        }

        return sourceMedia;
    }

    private int getInt(@NonNull MediaFormat mediaFormat, @NonNull String key) {
        if (mediaFormat.containsKey(key)) {
            return mediaFormat.getInteger(key);
        }
        return -1;
    }

    private long getLong(@NonNull MediaFormat mediaFormat, @NonNull String key) {
        if (mediaFormat.containsKey(key)) {
            return mediaFormat.getLong(key);
        }
        return -1;
    }
}
