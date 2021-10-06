/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.linkedin.android.litr.TransformationListener;
import com.linkedin.android.litr.analytics.TrackTransformationInfo;
import com.linkedin.android.litr.utils.TrackMetadataUtil;

import java.util.List;

public class MediaTransformationListener implements TransformationListener {

    private final Context context;
    private final String requestId;
    private final TransformationState transformationState;
    private final SharedMediaStoragePublisher publisher;
    private final TargetMedia targetMedia;

    public MediaTransformationListener(@NonNull Context context,
                                       @NonNull String requestId,
                                       @NonNull TransformationState transformationState,
                                       @NonNull TargetMedia targetMedia) {
        this(context, requestId, transformationState, targetMedia, new SharedMediaStoragePublisher(context));
    }

    @VisibleForTesting
    MediaTransformationListener(@NonNull Context context,
                                       @NonNull String requestId,
                                       @NonNull TransformationState transformationState,
                                       @NonNull TargetMedia targetMedia,
                                       @NonNull SharedMediaStoragePublisher publisher) {
        this.context = context;
        this.requestId = requestId;
        this.transformationState = transformationState;
        this.targetMedia = targetMedia;
        this.publisher = publisher;
    }

    @Override
    public void onStarted(@NonNull String id) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_RUNNING);
        }
    }

    @Override
    public void onProgress(@NonNull String id, float progress) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setProgress((int) (progress * TransformationState.MAX_PROGRESS));
        }
    }

    @Override
    public void onCompleted(@NonNull String id, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_COMPLETED);
            transformationState.setProgress(TransformationState.MAX_PROGRESS);
            transformationState.setStats(TrackMetadataUtil.printTransformationStats(context, trackTransformationInfos));
            publisher.publish(targetMedia.targetFile, false, (file, contentUri) -> targetMedia.setContentUri(contentUri));
        }
    }

    @Override
    public void onCancelled(@NonNull String id, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_CANCELLED);
            transformationState.setStats(TrackMetadataUtil.printTransformationStats(context, trackTransformationInfos));
        }
    }

    @Override
    public void onError(@NonNull String id,
                        @Nullable Throwable cause,
                        @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_ERROR);
            transformationState.setStats(TrackMetadataUtil.printTransformationStats(context, trackTransformationInfos));
        }
    }

}
