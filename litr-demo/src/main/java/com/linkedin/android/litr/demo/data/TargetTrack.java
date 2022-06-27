/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;

public class TargetTrack extends BaseObservable {
    public int sourceTrackIndex;
    public boolean shouldInclude;
    public boolean shouldTranscode;
    public boolean shouldApplyOverlay;
    public Uri overlay;
    public MediaTrackFormat format;

    public TargetTrack(int sourceTrackIndex, boolean shouldInclude, boolean shouldTranscode, @NonNull MediaTrackFormat format) {
        this.sourceTrackIndex = sourceTrackIndex;
        this.shouldInclude = shouldInclude;
        this.shouldTranscode = shouldTranscode;
        this.format = format;
    }
}
