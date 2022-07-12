/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.linkedin.android.litr.demo.BaseTransformationFragment;
import com.linkedin.android.litr.demo.MediaPickerListener;

public class TranscodingConfigPresenter {

    private BaseTransformationFragment fragment;
    private TargetMedia targetMedia;

    public TranscodingConfigPresenter(@NonNull BaseTransformationFragment fragment, @NonNull TargetMedia targetMedia) {
        this.fragment = fragment;
        this.targetMedia = targetMedia;
    }

    public void onIncludeTrackChanged(@NonNull TargetTrack targetTrack, boolean include) {
        targetTrack.shouldInclude = include;
        targetTrack.notifyChange();
        targetMedia.notifyChange();
    }

    public void onTranscodeTrackChanged(@NonNull TargetTrack targetTrack, boolean transcode) {
        if (targetTrack instanceof TargetVideoTrack) {
            ((TargetVideoTrack) targetTrack).shouldTranscode = transcode;
        } else if (targetTrack instanceof TargetAudioTrack) {
            ((TargetAudioTrack) targetTrack).shouldTranscode = transcode;
        }
        targetTrack.notifyChange();
    }

    public void onApplyOverlayChanged(@NonNull TargetTrack targetTrack, boolean applyOverlay) {
        targetTrack.shouldApplyOverlay = applyOverlay;
        targetTrack.notifyChange();
    }

    public void onPickOverlayClicked(@NonNull MediaPickerListener mediaPickerListener) {
        fragment.pickOverlay(mediaPickerListener);
    }

    public void onPickAudioOverlayClicked(@NonNull MediaPickerListener mediaPickerListener) {
        fragment.pickAudio(mediaPickerListener);
    }

    public Context getContext() {
        return fragment.getContext();
    }

    @BindingAdapter("overlayThumbnail")
    public static void loadImage(ImageView view, String imageUrl) {
        Glide.with(view.getContext())
             .load(imageUrl)
             .into(view);
    }
}
