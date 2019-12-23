/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.net.Uri;
import android.widget.CompoundButton;
import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;

public class OverlayTarget extends BaseObservable implements CompoundButton.OnCheckedChangeListener {

    public boolean shouldApplyOverlay;
    public Uri uri;
    public long size;

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        shouldApplyOverlay = b;
        notifyChange();
    }

    public void setUri(@Nullable Uri uri) {
        this.uri = uri;
        notifyChange();
    }

    public void setSize(long size) {
        this.size = size;
        notifyChange();
    }
}
