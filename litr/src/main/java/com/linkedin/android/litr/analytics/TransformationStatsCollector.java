/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.analytics;

import android.media.MediaFormat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TransformationStatsCollector {
    private List<TrackTransformationInfo> trackTransformationInfos;

    public TransformationStatsCollector() {
        trackTransformationInfos = new ArrayList<>(2);
    }

    @NonNull
    public List<TrackTransformationInfo> getStats() {
        return trackTransformationInfos;
    }

    public void addSourceTrack(@NonNull MediaFormat sourceMediaFormat) {
        TrackTransformationInfo trackTransformationInfo = new TrackTransformationInfo();
        trackTransformationInfo.setSourceFormat(sourceMediaFormat);

        trackTransformationInfos.add(trackTransformationInfo);
    }

    public void setTrackCodecs(int track, @Nullable String decoderCodec, @Nullable String encoderCodec) {
        TrackTransformationInfo trackTransformationInfo = trackTransformationInfos.get(track);
        trackTransformationInfo.setDecoderCodec(decoderCodec);
        trackTransformationInfo.setEncoderCodec(encoderCodec);
    }

    public void setTargetFormat(int track, @Nullable MediaFormat mediaFormat) {
        trackTransformationInfos.get(track).setTargetFormat(mediaFormat);
    }

    public void increaseTrackProcessingDuration(int track, long frameProcessingDuration) {
        TrackTransformationInfo trackTransformationInfo = trackTransformationInfos.get(track);
        long duration = trackTransformationInfo.getDuration();
        trackTransformationInfo.setDuration(duration + frameProcessingDuration);
    }
}
