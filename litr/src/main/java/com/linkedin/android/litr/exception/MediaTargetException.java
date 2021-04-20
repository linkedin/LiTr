/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.exception;

import android.net.Uri;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

public class MediaTargetException extends MediaTransformationException {

    private static final String INVALID_PARAMS_TEXT = "Invalid parameters";
    private static final String IO_FAILURE_TEXT = "Failed to open the media target for write.";
    private static final String UNSUPPORTED_URI_TYPE_TEXT = "URI type not supported at API level below 26";

    private final Error error;
    private final String outputFilePath;
    private final int outputFormat;

    public MediaTargetException(@NonNull Error error, @NonNull Uri outputFileUri, @IntRange(from=0, to=2) int outputFormat, @NonNull Throwable cause) {
        this(error, outputFileUri.toString(), outputFormat, cause);
    }

    public MediaTargetException(@NonNull Error error, @NonNull String outputFilePath, @IntRange(from=0, to=2) int outputFormat, @NonNull Throwable cause) {
        super(cause);
        this.error = error;
        this.outputFilePath = outputFilePath;
        this.outputFormat = outputFormat;
    }

    public enum Error {
        INVALID_PARAMS(INVALID_PARAMS_TEXT),
        IO_FAILUE(IO_FAILURE_TEXT),
        UNSUPPORTED_URI_TYPE(UNSUPPORTED_URI_TYPE_TEXT);

        private final String text;
        Error(String text) {
            this.text = text;
        }
    }

    @NonNull
    public Error getError() {
        return error;
    }

    @Override
    @NonNull
    public String toString() {
        return super.toString() + '\n'
            + error.text + '\n'
            + "Output file path or Uri encoded string: " + outputFilePath + '\n'
            + "MediaMuxer output format: " + outputFormat;
    }

}
