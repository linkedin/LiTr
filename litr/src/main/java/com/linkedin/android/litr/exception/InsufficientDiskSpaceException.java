/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.exception;

import androidx.annotation.NonNull;

import java.util.Locale;

public class InsufficientDiskSpaceException extends MediaTransformationException {
    private final long estimatedTargetFileSizeInBytes;
    private final long availableDiskSpaceInBytes;

    public InsufficientDiskSpaceException(long estimatedTargetFileSizeInBytes, long availableDiskSpaceInBytes) {
        this(estimatedTargetFileSizeInBytes, availableDiskSpaceInBytes, new Throwable());
    }

    public InsufficientDiskSpaceException(long estimatedTargetFileSizeInBytes, long availableDiskSpaceInBytes, @NonNull Throwable cause) {
        super(cause);
        this.estimatedTargetFileSizeInBytes = estimatedTargetFileSizeInBytes;
        this.availableDiskSpaceInBytes = availableDiskSpaceInBytes;
    }

    @Override
    @NonNull
    public String getMessage() {
        return String.format(Locale.ENGLISH, "Insufficient disk space, estimated file size in bytes %d, available disk space in bytes %d",
                             estimatedTargetFileSizeInBytes,
                             availableDiskSpaceInBytes);
    }
}
