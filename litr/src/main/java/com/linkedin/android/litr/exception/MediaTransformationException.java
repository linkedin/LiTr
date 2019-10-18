/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.exception;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class MediaTransformationException extends Exception {

    @NonNull private String jobId;

    public MediaTransformationException(@Nullable Throwable cause) {
        super(cause);
    }

    public void setJobId(@NonNull String jobId) {
        this.jobId = jobId;
    }

    @Override
    @NonNull
    public String toString() {
        return super.toString() + "Media transformation failed for job id: " + jobId;
    }
}
