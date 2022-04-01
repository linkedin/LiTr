/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.test;

import android.content.Context;
import android.media.MediaFormat;
import android.net.Uri;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.TrackTransform;
import com.linkedin.android.litr.TransformationListener;
import com.linkedin.android.litr.TransformationOptions;

import java.util.List;

/**
 * Mock transformer, meant to be used only in tests. Doesn't do any actual transformations.
 */
public class MockMediaTransformer extends MediaTransformer {

    private List<TransformationEvent> transformationEvents;

    public MockMediaTransformer(@NonNull Context context) {
        super(context, null, null);
    }

    public void setEvents(@NonNull List<TransformationEvent> transformationEvents) {
        this.transformationEvents = transformationEvents;
    }

    @Override
    public void transform(@NonNull String requestId,
                          @NonNull Uri inputUri,
                          @NonNull Uri outputUri,
                          @Nullable MediaFormat targetVideoFormat,
                          @Nullable MediaFormat targetAudioFormat,
                          @NonNull TransformationListener listener,
                          @Nullable TransformationOptions transformationOptions) {
        playEvents(listener);
    }

    @Override
    public void transform(@NonNull String requestId,
                          List<TrackTransform> trackTransforms,
                          @NonNull TransformationListener listener,
                          @IntRange(from = GRANULARITY_NONE) int granularity) {
        playEvents(listener);
    }


    @Override
    public void cancel(@NonNull String requestId) {}

    @Override
    public void release() {}

    @Override
    public long getEstimatedTargetVideoSize(@NonNull Uri inputUri,
                                            @NonNull MediaFormat targetVideoFormat,
                                            @Nullable MediaFormat targetAudioFormat,
                                            @Nullable TransformationOptions transformationOptions) {
        return 0;
    }

    private void playEvents(@NonNull TransformationListener transformationListener) {
        if (transformationEvents == null) {
            // nothing to do
            return;
        }

        for (TransformationEvent event : transformationEvents) {
            switch (event.type) {
                case TransformationEvent.TYPE_START:
                    transformationListener.onStarted(event.id);
                    break;
                case TransformationEvent.TYPE_PROGRESS:
                    transformationListener.onProgress(event.id, event.progress);
                    break;
                case TransformationEvent.TYPE_COMPLETED:
                    transformationListener.onCompleted(event.id, null);
                    break;
                case TransformationEvent.TYPE_ERROR:
                    transformationListener.onError(event.id, event.cause, null);
                    break;
                case TransformationEvent.TYPE_CANCELLED:
                    transformationListener.onCancelled(event.id, null);
                    break;
            }
        }
    }
}
