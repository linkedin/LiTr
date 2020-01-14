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
import com.linkedin.android.litr.demo.data.AudioTrackFormat;
import com.linkedin.android.litr.demo.data.TargetAudioTrack;
import com.linkedin.android.litr.demo.data.TranscodingConfigPresenter;
import com.linkedin.android.litr.demo.databinding.ItemAudioTrackBinding;

public class AudioTrackViewHolder extends RecyclerView.ViewHolder {

    private ItemAudioTrackBinding binding;

    public AudioTrackViewHolder(@NonNull ItemAudioTrackBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(@NonNull TranscodingConfigPresenter presenter,
                     @NonNull AudioTrackFormat sourceTrackFormat,
                     @NonNull TargetAudioTrack targetTrack) {
        binding.setTargetTrack(targetTrack);
        binding.setPresenter(presenter);
        binding.setAudioTrack(sourceTrackFormat);
        binding.executePendingBindings();
    }
}
