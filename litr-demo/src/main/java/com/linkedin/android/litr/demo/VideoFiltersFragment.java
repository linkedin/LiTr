/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.codec.MediaCodecDecoder;
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.demo.data.TargetMedia;
import com.linkedin.android.litr.demo.data.TransformationPresenter;
import com.linkedin.android.litr.demo.data.TransformationState;
import com.linkedin.android.litr.demo.databinding.FragmentVideoFiltersBinding;
import com.linkedin.android.litr.exception.MediaSourceException;
import com.linkedin.android.litr.io.MediaExtractorMediaSource;
import com.linkedin.android.litr.io.MediaRange;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaSourceFactory;
import com.linkedin.android.litr.render.GlThumbnailRenderer;
import com.linkedin.android.litr.thumbnails.ThumbnailExtractListener;
import com.linkedin.android.litr.thumbnails.ThumbnailExtractParameters;
import com.linkedin.android.litr.thumbnails.VideoThumbnailExtractor;
import com.linkedin.android.litr.utils.TranscoderUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class VideoFiltersFragment extends BaseTransformationFragment implements MediaPickerListener {

    private FragmentVideoFiltersBinding binding;

    private MediaTransformer mediaTransformer;
    private TargetMedia targetMedia;

    private ArrayAdapter<DemoFilter> adapter;

    private VideoThumbnailExtractor thumbnailExtractor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaTransformer.release();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoFiltersBinding.inflate(inflater, container, false);

        SourceMedia sourceMedia = new SourceMedia();
        binding.setSourceMedia(sourceMedia);

        binding.sectionPickVideo.buttonPickVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickVideo(VideoFiltersFragment.this);
            }
        });

        binding.spinnerFilters.setAdapter(adapter);
        binding.spinnerFilters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                targetMedia.filter = adapter.getItem(position).filter;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.setTransformationState(new TransformationState());
        binding.setTransformationPresenter(new TransformationPresenter(getContext(), mediaTransformer));

        binding.setTargetMedia(targetMedia);

        return binding.getRoot();
    }

    @Override
    public void onMediaPicked(@NonNull Uri uri) {
        SourceMedia sourceMedia = binding.getSourceMedia();
        updateSourceMedia(sourceMedia, uri);

        Point sourceSize = TranscoderUtils.getVideoDimensions(requireContext().getApplicationContext(), sourceMedia.uri);

        if (sourceSize == null) {
            return;
        }

        List<Long> timestamps = new ArrayList<>();
        for (long i = 0; i <= 10000000L; i += 100000L) {
            timestamps.add(i);
        }

//        thumbnailExtractor.extract(UUID.randomUUID().toString(), new ThumbnailExtractParameters(
//                mediaSourceFactory,
//                timestamps,
//                new MediaRange(0, Long.MAX_VALUE),
//                new MediaCodecDecoder(),
//                sourceSize,
//                new GlThumbnailRenderer(null)
//        ));

//        File targetFile = new File(TransformationUtil.getTargetFileDirectory(requireContext().getApplicationContext()),
//                              "transcoded_" + TransformationUtil.getDisplayName(getContext(), sourceMedia.uri));
//        binding.getTargetMedia().setTargetFile(targetFile);
//        binding.getTargetMedia().setTracks(sourceMedia.tracks);
//
//        binding.getTransformationState().setState(TransformationState.STATE_IDLE);
//        binding.getTransformationState().setStats(null);
    }

}
