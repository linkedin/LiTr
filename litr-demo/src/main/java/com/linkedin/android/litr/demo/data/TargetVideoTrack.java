/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.view.View;
import android.widget.AdapterView;

public class TargetVideoTrack extends TargetTrack {

    public TargetVideoTrack(int sourceTrackIndex,
                            boolean shouldInclude,
                            boolean shouldTranscode,
                            VideoTrackFormat format) {
        super(sourceTrackIndex, shouldInclude, shouldTranscode, format);
    }

    public VideoTrackFormat getTrackFormat() {
        return (VideoTrackFormat) format;
    }

    public void onMimeTypeSelected(AdapterView<?> parent, View view, int pos, long id) {
        format.mimeType = (String) parent.getAdapter().getItem(pos);
    }
}
