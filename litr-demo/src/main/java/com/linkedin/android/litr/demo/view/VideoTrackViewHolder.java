/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.view;

import android.net.Uri;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.linkedin.android.litr.demo.MediaPickerListener;
import com.linkedin.android.litr.demo.data.TargetVideoTrack;
import com.linkedin.android.litr.demo.data.TranscodingConfigPresenter;
import com.linkedin.android.litr.demo.data.VideoTrackFormat;
import com.linkedin.android.litr.demo.databinding.ItemVideoTrackBinding;

public class VideoTrackViewHolder extends RecyclerView.ViewHolder implements MediaPickerListener {

    private ItemVideoTrackBinding binding;

    public VideoTrackViewHolder(@NonNull ItemVideoTrackBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(@NonNull final TranscodingConfigPresenter presenter,
                     @NonNull VideoTrackFormat videoTrackFormat,
                     @NonNull TargetVideoTrack targetTrack) {
        binding.setPresenter(presenter);
        binding.setVideoTrack(videoTrackFormat);
        binding.setTargetTrack(targetTrack);
        binding.executePendingBindings();

        binding.buttonPickVideoOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onPickOverlayClicked(VideoTrackViewHolder.this);
            }
        });
    }

    @Override
    public void onMediaPicked(@NonNull Uri uri) {
        binding.getTargetTrack().overlay = uri;
        binding.getTargetTrack().notifyChange();
    }
}
