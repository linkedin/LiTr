/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.net.Uri;

public class TargetVideoTrack extends TargetTrack {

    public boolean shouldApplyOverlay;
    public Uri overlay;

    public TargetVideoTrack(int sourceTrackIndex,
                            boolean shouldInclude,
                            boolean shouldTranscode,
                            VideoTrackFormat format) {
        super(sourceTrackIndex, shouldInclude, shouldTranscode, format);
    }

    public VideoTrackFormat getTrackFormat() {
        return (VideoTrackFormat) format;
    }
}
