/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import androidx.annotation.NonNull;

public class AudioTrackFormat extends MediaTrackFormat {

    public int channelCount;
    public int samplingRate;
    public int bitrate;
    public long duration;

    public AudioTrackFormat(int index, @NonNull String mimeType) {
        super(index, mimeType);
    }

    public AudioTrackFormat(@NonNull AudioTrackFormat audioTrackFormat) {
        super(audioTrackFormat);
        this.channelCount = audioTrackFormat.channelCount;
        this.samplingRate = audioTrackFormat.samplingRate;
        this.bitrate = audioTrackFormat.bitrate;
        this.duration = audioTrackFormat.duration;
    }
}
