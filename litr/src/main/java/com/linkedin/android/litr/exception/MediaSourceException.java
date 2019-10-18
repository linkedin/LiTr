/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.exception;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MediaSourceException extends MediaTransformationException {

    private static final String DATA_SOURCE_ERROR_TEXT = "data source error";
    private static final String MEDIA_EXTRACTOR_CREATION_ERROR_TEXT = "Failed to create media source due to a ";

    @NonNull private final Error error;
    @Nullable private final Uri inputUri;

    public MediaSourceException(@NonNull Error error,
                                @Nullable Uri inputUri,
                                @NonNull Throwable throwable) {
        super(throwable);
        this.error = error;
        this.inputUri = inputUri;
    }

    public enum Error {
        DATA_SOURCE(DATA_SOURCE_ERROR_TEXT);

        private final String text;

        Error(String message) {
            this.text = message;
        }
    }

    @NonNull
    public Error getError() {
        return error;
    }

    @Override
    @NonNull
    public String getMessage() {
        return MEDIA_EXTRACTOR_CREATION_ERROR_TEXT + error.text;
    }

    @Override
    @NonNull
    public String toString() {
        return super.toString() + '\n'
            + MEDIA_EXTRACTOR_CREATION_ERROR_TEXT + error.text + '\n'
            + "Uri: " + inputUri;
    }
}
