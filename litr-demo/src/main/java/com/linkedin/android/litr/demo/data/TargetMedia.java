/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;

import com.linkedin.android.litr.filter.GlFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TargetMedia extends BaseObservable {

    public static final int DEFAULT_VIDEO_WIDTH = 1280;
    public static final int DEFAULT_VIDEO_HEIGHT = 720;
    public static final int DEFAULT_VIDEO_BITRATE = 5000000;
    public static final int DEFAULT_KEY_FRAME_INTERVAL = 5;
    public static final int DEFAULT_AUDIO_BITRATE = 128000;

    public File targetFile;
    @Nullable
    private Uri contentUri;
    public List<TargetTrack> tracks = new ArrayList<>();
    public Uri backgroundImageUri;
    public GlFilter filter;
    public boolean writeToWav;

    public void setTracks(@NonNull List<MediaTrackFormat> sourceTracks) {
        tracks = new ArrayList<>(sourceTracks.size());
        for (MediaTrackFormat sourceTrackFormat : sourceTracks) {
            TargetTrack targetTrack;
            if (sourceTrackFormat instanceof VideoTrackFormat) {
                VideoTrackFormat trackFormat = new VideoTrackFormat((VideoTrackFormat) sourceTrackFormat);
                trackFormat.width = DEFAULT_VIDEO_WIDTH;
                trackFormat.height = DEFAULT_VIDEO_HEIGHT;
                trackFormat.bitrate = DEFAULT_VIDEO_BITRATE;
                trackFormat.keyFrameInterval = DEFAULT_KEY_FRAME_INTERVAL;
                targetTrack = new TargetVideoTrack(sourceTrackFormat.index,
                                                   true,
                                                   false,
                                                   trackFormat);
            } else if (sourceTrackFormat instanceof AudioTrackFormat) {
                AudioTrackFormat trackFormat = new AudioTrackFormat((AudioTrackFormat) sourceTrackFormat);
                trackFormat.bitrate = DEFAULT_AUDIO_BITRATE;
                targetTrack = new TargetAudioTrack(sourceTrackFormat.index,
                                                   true,
                                                   false,
                                                   trackFormat);
            } else {
                targetTrack = new TargetTrack(sourceTrackFormat.index,
                                              true,
                                              false,
                                              new MediaTrackFormat(sourceTrackFormat));
            }
            tracks.add(targetTrack);
        }
        notifyChange();
    }

    public void setTargetFile(@NonNull File targetFile) {
        this.targetFile = targetFile;
        notifyChange();
    }

    public int getIncludedTrackCount() {
        int trackCount = 0;
        for (TargetTrack track : tracks) {
            if (track.shouldInclude) {
                trackCount++;
            }
        }
        return trackCount;
    }

    public void setOverlayImageUri(@NonNull Uri overlayImageUri) {
        for (TargetTrack targetTrack : tracks) {
            if (targetTrack instanceof TargetVideoTrack) {
                ((TargetVideoTrack) targetTrack).overlay = overlayImageUri;
            }
        }
        notifyChange();
    }

    @Nullable
    public Uri getVideoOverlay() {
        for (TargetTrack targetTrack : tracks) {
            if ((targetTrack instanceof TargetVideoTrack) && ((TargetVideoTrack) targetTrack).overlay != null) {
                return ((TargetVideoTrack) targetTrack).overlay;
            }
        }
        return null;
    }

    @Nullable
    public Uri getContentUri() {
        return contentUri;
    }

    public void setContentUri(@Nullable Uri storedContentUri) {
        this.contentUri = storedContentUri;
        notifyChange();
    }

}
