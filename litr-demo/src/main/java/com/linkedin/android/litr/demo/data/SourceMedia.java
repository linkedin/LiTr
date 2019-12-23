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

public class SourceMedia extends BaseObservable {

    public Uri uri;
    public long size;

    public int videoTrack;
    public String videoMimeType;
    public int videoWidth;
    public int videoHeight;
    public int videoBitrate;
    public int videoFrameRate;
    public int videoKeyFrameInterval;
    public long videoDuration;
    public int videoRotation;

    public int audioTrack;
    public String audioMimeType;
    public int audioChannelCount;
    public int audioSamplingRate;
    public int audioBitrate;
    public long audioDuration;
}
