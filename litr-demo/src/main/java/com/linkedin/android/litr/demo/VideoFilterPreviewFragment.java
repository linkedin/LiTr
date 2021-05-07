/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.video.VideoListener;
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.demo.databinding.FragmentVideoFilterPreviewBinding;
import com.linkedin.android.litr.filter.GlFrameRenderFilter;
import com.linkedin.android.litr.preview.VideoPreviewRenderer;

public class VideoFilterPreviewFragment extends BaseTransformationFragment implements MediaPickerListener {

    private FragmentVideoFilterPreviewBinding binding;

    private ArrayAdapter<DemoFilter> adapter;

    private DataSource.Factory dataSourceFactory;
    private SimpleExoPlayer exoPlayer;

    private VideoPreviewRenderer renderer;

    private VideoListener videoListener = new VideoListener() {
        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            if (binding != null) {
                binding.videoFrame.setAspectRatio((float) width / height);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, DemoFilter.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Context context = getContext().getApplicationContext();
        dataSourceFactory = new DefaultDataSourceFactory(context, "LiTr");
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);
        exoPlayer = new SimpleExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build();
        exoPlayer.setThrowsWhenUsingWrongThread(false);
        renderer = new VideoPreviewRenderer(surfaceTexture -> exoPlayer.setVideoSurface(new Surface(surfaceTexture)));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoFilterPreviewBinding.inflate(inflater, container, false);

        SourceMedia sourceMedia = new SourceMedia();
        binding.setSourceMedia(sourceMedia);

        binding.sectionPickVideo.buttonPickVideo.setOnClickListener(view -> pickVideo(VideoFilterPreviewFragment.this));

        binding.spinnerFilters.setAdapter(adapter);
        binding.spinnerFilters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderer.setFilter((GlFrameRenderFilter) adapter.getItem(position).filter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        exoPlayer.setVideoSurfaceView(binding.videoPreview);
        exoPlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
        exoPlayer.addVideoListener(videoListener);

        binding.videoPreview.setRenderer(renderer);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        exoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        exoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        exoPlayer.removeVideoListener(videoListener);
        renderer.release();
        exoPlayer.release();
    }

    @Override
    public void onMediaPicked(@NonNull Uri uri) {
        SourceMedia sourceMedia = binding.getSourceMedia();
        updateSourceMedia(sourceMedia, uri);

        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);
    }
}
