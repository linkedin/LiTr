/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import androidx.annotation.NonNull;

public class MediaTrackFormat {

    public int index;
    public String mimeType;

    MediaTrackFormat(int index, @NonNull String mimeType) {
        this.index = index;
        this.mimeType = mimeType;
    }

    MediaTrackFormat(@NonNull MediaTrackFormat mediaTrackFormat) {
        this.index = mediaTrackFormat.index;
        this.mimeType = mediaTrackFormat.mimeType;
    }
}
