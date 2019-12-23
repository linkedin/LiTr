/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.widget.CompoundButton;
import androidx.databinding.BaseObservable;

public class VideoTarget extends BaseObservable implements CompoundButton.OnCheckedChangeListener {

    public boolean shouldTranscodeVideo;
    public String targetWidth;
    public String targetHeight;
    public String targetBitrate;
    public String targetKeyFrameInterval;

    public VideoTarget() {
        targetWidth = "1280";
        targetHeight = "720";
        targetBitrate = "3.3";
        targetKeyFrameInterval = "5";
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        shouldTranscodeVideo = b;
        notifyChange();
    }
}
