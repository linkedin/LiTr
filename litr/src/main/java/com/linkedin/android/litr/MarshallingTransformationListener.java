/*
 * Copyright 2020 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.analytics.TrackTransformationInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

class MarshallingTransformationListener {
    private static final String TAG = MarshallingTransformationListener.class.getSimpleName();

    private static final int EVENT_STARTED = 0;
    private static final int EVENT_COMPLETED = 1;
    private static final int EVENT_ERROR = 2;
    private static final int EVENT_PROGRESS = 3;
    private static final int EVENT_CANCELLED = 4;

    private static final String KEY_JOB_ID = "jobId";
    private static final String KEY_PROGRESS = "progress";
    private static final String KEY_THROWABLE = "throwable";

    private final Map<String, Future<?>> futureMap;
    private final TransformationListener listener;

    private Bundle data = new Bundle();
    private MarshallingHandler handler;

    MarshallingTransformationListener(@NonNull Map<String, Future<?>> futureMap,
                                      @NonNull final TransformationListener listener,
                                      @Nullable Looper looper) {
        this.futureMap = futureMap;
        this.listener = listener;

        if (looper != null) {
            handler = new MarshallingHandler(looper, listener);
        }
    }

    void onStarted(@NonNull String jobId) {
        if (handler == null) {
            listener.onStarted(jobId);
        } else {
            Message msg = Message.obtain(handler, EVENT_STARTED);
            msg.obj = null;
            data.putString(KEY_JOB_ID, jobId);
            msg.setData(data);
            msg.sendToTarget();
        }
    }

    void onCompleted(@NonNull String jobId,
                     @NonNull final List<TrackTransformationInfo> trackTransformationInfos) {
        futureMap.remove(jobId);

        if (handler == null) {
            listener.onCompleted(jobId, trackTransformationInfos);
        } else {
            Message msg = Message.obtain(handler, EVENT_COMPLETED);
            msg.obj = trackTransformationInfos;
            data.putString(KEY_JOB_ID, jobId);
            msg.setData(data);
            msg.sendToTarget();
        }
    }

    void onCancelled(@NonNull final String jobId,
                     @NonNull final List<TrackTransformationInfo> trackTransformationInfos) {
        futureMap.remove(jobId);

        if (handler == null) {
            listener.onCancelled(jobId, trackTransformationInfos);
        } else {
            Message msg = Message.obtain(handler, EVENT_CANCELLED);
            msg.obj = trackTransformationInfos;
            data.putString(KEY_JOB_ID, jobId);
            msg.setData(data);
            msg.sendToTarget();
        }
    }

    void onError(@NonNull final String jobId,
                 @Nullable final Throwable cause,
                 @NonNull final List<TrackTransformationInfo> trackTransformationInfos) {
        futureMap.remove(jobId);

        if (handler == null) {
            listener.onError(jobId, cause, trackTransformationInfos);
        } else {
            Message msg = Message.obtain(handler, EVENT_ERROR);
            msg.obj = trackTransformationInfos;
            data.putString(KEY_JOB_ID, jobId);
            data.putSerializable(KEY_THROWABLE, cause);
            msg.setData(data);
            msg.sendToTarget();
        }
    }

    void onProgress(@NonNull String jobId,
                    float progress) {
        if (handler == null) {
            listener.onProgress(jobId, progress);
        } else {
            Message msg = Message.obtain(handler, EVENT_PROGRESS);
            msg.obj = null;
            data.putString(KEY_JOB_ID, jobId);
            data.putFloat(KEY_PROGRESS, progress);
            msg.setData(data);
            msg.sendToTarget();
        }
    }

    private static class MarshallingHandler extends Handler {

        private final TransformationListener listener;

        private MarshallingHandler(@NonNull Looper looper, @NonNull TransformationListener listener) {
            super(looper);
            this.listener = listener;
        }

        @Override
        public void handleMessage(@NonNull Message message) {
            List<TrackTransformationInfo> trackTransformationInfos = message.obj == null ? null : (List<TrackTransformationInfo>) message.obj;

            Bundle data = message.getData();
            String jobId = data.getString(KEY_JOB_ID);
            if (jobId == null) {
                throw new IllegalArgumentException("Handler message doesn't contain an id!");
            }

            switch (message.what) {
                case EVENT_STARTED: {
                    listener.onStarted(jobId);
                    break;
                }
                case EVENT_COMPLETED: {
                    listener.onCompleted(jobId, trackTransformationInfos);
                    break;
                }
                case EVENT_CANCELLED: {
                    listener.onCancelled(jobId, trackTransformationInfos);
                    break;
                }
                case EVENT_ERROR: {
                    Throwable cause = (Throwable) data.getSerializable(KEY_THROWABLE);
                    listener.onError(jobId, cause, trackTransformationInfos);
                    break;
                }
                case EVENT_PROGRESS: {
                    float progress = data.getFloat(KEY_PROGRESS);
                    listener.onProgress(jobId, progress);
                    break;
                }
                default:
                    Log.e(TAG, "Unknown event received: " + message.what);
            }
        }
    }
}
