/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.net.Uri;
import androidx.databinding.BaseObservable;

import java.util.ArrayList;
import java.util.List;

public class SourceMedia extends BaseObservable {

    public Uri uri;
    public long size;
    public float duration;

    public List<MediaTrackFormat> tracks = new ArrayList<>();

    public boolean hasAudio() {
        for (MediaTrackFormat trackFormat : tracks) {
            if (trackFormat instanceof AudioTrackFormat) {
                return true;
            }
        }
        return false;
    }
}
