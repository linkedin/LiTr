/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.test;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class TransformationEvent {

    public static final int TYPE_START = 0;
    public static final int TYPE_PROGRESS = 1;
    public static final int TYPE_COMPLETED = 2;
    public static final int TYPE_ERROR = 3;
    public static final int TYPE_CANCELLED = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ TYPE_START, TYPE_PROGRESS, TYPE_COMPLETED, TYPE_ERROR, TYPE_CANCELLED})
    @interface EventType {}

    @NonNull public final String id;
    @EventType public final int type;
    public final float progress;
    @Nullable public final Throwable cause;

    public TransformationEvent(@NonNull String id,
                               @EventType int type,
                               @FloatRange(from = 0, to = 1) float progress,
                               @Nullable Throwable cause) {
        this.id = id;
        this.type = type;
        this.progress = progress;
        this.cause = cause;
    }
}
