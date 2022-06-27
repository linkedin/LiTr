/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.linkedin.android.litr.demo.MediaPickerListener;
import com.linkedin.android.litr.demo.data.AudioTrackFormat;
import com.linkedin.android.litr.demo.data.TargetAudioTrack;
import com.linkedin.android.litr.demo.data.TranscodingConfigPresenter;
import com.linkedin.android.litr.demo.databinding.ItemAudioTrackBinding;

public class AudioTrackViewHolder extends RecyclerView.ViewHolder implements MediaPickerListener {

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

        binding.buttonPickAudioOverlay.setOnClickListener( view ->
                presenter.onPickAudioOverlayClicked(AudioTrackViewHolder.this)
        );

        binding.playAudioOverlay.setOnClickListener( view -> {
            Context context = presenter.getContext();
            Uri audioOverlayUri = binding.getTargetTrack().overlay;

            if (context != null && audioOverlayUri != null) {
                Intent playIntent = new Intent(Intent.ACTION_VIEW);
                playIntent.setDataAndType(audioOverlayUri, context.getContentResolver().getType(audioOverlayUri));
                context.startActivity(playIntent);
            }
        });
    }

    @Override
    public void onMediaPicked(@NonNull Uri uri) {
        binding.getTargetTrack().overlay = uri;
        binding.getTargetTrack().notifyChange();
    }
}
