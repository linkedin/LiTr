/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.linkedin.android.litr.demo.data.AudioTrackFormat;
import com.linkedin.android.litr.demo.data.GenericTrackFormat;
import com.linkedin.android.litr.demo.data.MediaTrackFormat;
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.demo.data.TargetAudioTrack;
import com.linkedin.android.litr.demo.data.TargetMedia;
import com.linkedin.android.litr.demo.data.TargetVideoTrack;
import com.linkedin.android.litr.demo.data.TranscodingConfigPresenter;
import com.linkedin.android.litr.demo.data.VideoTrackFormat;
import com.linkedin.android.litr.demo.databinding.ItemAudioTrackBinding;
import com.linkedin.android.litr.demo.databinding.ItemGenericTrackBinding;
import com.linkedin.android.litr.demo.databinding.ItemVideoTrackBinding;
import com.linkedin.android.litr.demo.view.AudioTrackViewHolder;
import com.linkedin.android.litr.demo.view.GenericTrackViewHolder;
import com.linkedin.android.litr.demo.view.VideoTrackViewHolder;

public class MediaTrackAdapter extends RecyclerView.Adapter {

    private static final int TYPE_UNKNOWN = -1;
    private static final int TYPE_VIDEO = 0;
    private static final int TYPE_AUDIO = 1;
    private static final int TYPE_GENERIC = 3;

    private SourceMedia sourceMedia;
    private TargetMedia targetMedia;
    private TranscodingConfigPresenter presenter;

    public MediaTrackAdapter(@NonNull TranscodingConfigPresenter presenter, @NonNull SourceMedia sourceMedia, @NonNull TargetMedia targetMedia) {
        this.sourceMedia = sourceMedia;
        this.targetMedia = targetMedia;
        this.presenter = presenter;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_VIDEO:
                return new VideoTrackViewHolder(ItemVideoTrackBinding.inflate(layoutInflater, parent, false));
            case TYPE_AUDIO:
                return new AudioTrackViewHolder(ItemAudioTrackBinding.inflate(layoutInflater, parent, false));
            case TYPE_GENERIC:
            default:
                return new GenericTrackViewHolder(ItemGenericTrackBinding.inflate(layoutInflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MediaTrackFormat mediaTrackFormat = sourceMedia.tracks.get(position);
        if (holder instanceof VideoTrackViewHolder) {
            ((VideoTrackViewHolder) holder).bind(
                    presenter,
                    (VideoTrackFormat) mediaTrackFormat,
                    (TargetVideoTrack) targetMedia.tracks.get(position));
        } else if (holder instanceof AudioTrackViewHolder) {
            ((AudioTrackViewHolder) holder).bind(
                    presenter,
                    (AudioTrackFormat) mediaTrackFormat,
                    (TargetAudioTrack) targetMedia.tracks.get(position));
        } else {
            ((GenericTrackViewHolder) holder).bind(
                    presenter,
                    mediaTrackFormat,
                    targetMedia.tracks.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return sourceMedia.tracks.size();
    }

    @Override
    public int getItemViewType(int position) {
        MediaTrackFormat trackFormat = sourceMedia.tracks.get(position);
        if (trackFormat instanceof VideoTrackFormat) {
            return TYPE_VIDEO;
        } else if (trackFormat instanceof AudioTrackFormat) {
            return TYPE_AUDIO;
        } else if (trackFormat instanceof GenericTrackFormat) {
            return TYPE_GENERIC;
        }
        return TYPE_UNKNOWN;
    }

}
