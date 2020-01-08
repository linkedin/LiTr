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

public class AudioTarget extends BaseObservable implements CompoundButton.OnCheckedChangeListener {

    public boolean shouldTranscodeAudio;
    public boolean shouldKeepTrack;
    public String targetBitrate;

    public AudioTarget() {
        targetBitrate = "128";
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean checked) {
        shouldTranscodeAudio = checked;
        shouldKeepTrack = checked;
        notifyChange();
    }
}
