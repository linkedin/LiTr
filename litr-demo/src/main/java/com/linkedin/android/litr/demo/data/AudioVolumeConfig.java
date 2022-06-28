/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.BindingAdapter;

import com.google.android.material.slider.Slider;

public class AudioVolumeConfig extends BaseObservable {

    public final Slider.OnChangeListener onValueChangeListener = (slider, value, fromUser) -> {
        this.value = slider.getValue();
    };

    public boolean enabled;
    public Float value = 1.0f;

    @Bindable
    public Boolean getEnabled() {
        return enabled;
    }

    @BindingAdapter(value = "onChangeListener")
    public static void setOnChangeListener(@NonNull Slider slider, @Nullable Slider.OnChangeListener onChangeListener) {
        slider.addOnChangeListener(onChangeListener);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
        notifyChange();
    }
}
