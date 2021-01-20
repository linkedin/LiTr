/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.view;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.linkedin.android.litr.demo.data.MediaTrackFormat;
import com.linkedin.android.litr.demo.data.TargetTrack;
import com.linkedin.android.litr.demo.data.TranscodingConfigPresenter;
import com.linkedin.android.litr.demo.databinding.ItemGenericTrackBinding;

public class GenericTrackViewHolder extends RecyclerView.ViewHolder {

    private ItemGenericTrackBinding binding;

    public GenericTrackViewHolder(@NonNull ItemGenericTrackBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(@NonNull TranscodingConfigPresenter presenter,
                     @NonNull MediaTrackFormat mediaTrackFormat,
                     @NonNull TargetTrack targetTrack) {
        binding.setPresenter(presenter);
        binding.setMediaTrack(mediaTrackFormat);
        binding.setTargetTrack(targetTrack);
        binding.executePendingBindings();
    }
}
