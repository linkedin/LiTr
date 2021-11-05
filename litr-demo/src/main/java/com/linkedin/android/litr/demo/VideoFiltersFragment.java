/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.graphics.Bitmap;
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
import com.linkedin.android.litr.render.GlThumbnailRenderer;
import com.linkedin.android.litr.thumbnails.ExtractFrameProvider;
import com.linkedin.android.litr.thumbnails.ThumbnailExtractListener;
import com.linkedin.android.litr.thumbnails.ThumbnailExtractParameters;
import com.linkedin.android.litr.thumbnails.VideoThumbnailExtractor;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VideoFiltersFragment extends BaseTransformationFragment implements MediaPickerListener {

    private FragmentVideoFiltersBinding binding;

    private MediaTransformer mediaTransformer;
    private TargetMedia targetMedia;

    private ArrayAdapter<DemoFilter> adapter;

    private VideoThumbnailExtractor thumbnailExtractor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaTransformer = new MediaTransformer(getContext().getApplicationContext());
        targetMedia = new TargetMedia();

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, DemoFilter.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        thumbnailExtractor = new VideoThumbnailExtractor(requireContext(), Executors.newSingleThreadExecutor(), new ThumbnailExtractListener() {
            @Override
            public void onStarted(@NonNull String id) {

            }

            @Override
            public void onExtracted(@NonNull String id, int index, int remaining) {

            }

            @Override
            public void onCompleted(@NonNull String id) {

            }

            @Override
            public void onCancelled(@NonNull String id) {

            }

            @Override
            public void onError(@NonNull String id, @Nullable Throwable cause) {

            }
        }, Looper.getMainLooper());
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

        try {
            GlThumbnailRenderer.ThumbnailReadyListener listener = new GlThumbnailRenderer.ThumbnailReadyListener() {
                @Override
                public void onThumbnailReady(String filePath) {
                    // TODO: Do something with this
                }

                @Override
                public void onThumbnailBitmapReady(Bitmap bitmap) {
                    // TODO: Do something with this
                }
            };

            ExtractFrameProvider frameProvider = new ExtractFrameProvider() {
                @NonNull
                @Override
                public MediaRange getRange() {
                    return new MediaRange(0, Long.MAX_VALUE);
                }

                long nextExtractTime = 0;

                @Override
                public boolean shouldExtract(long presentationTimeUs) {
                    return presentationTimeUs > nextExtractTime;

                }
                @Override
                public void didExtract(long presentationTimeUs) {
                    nextExtractTime = presentationTimeUs + TimeUnit.SECONDS.toMicros(1);
                }
            };

            thumbnailExtractor.extract(UUID.randomUUID().toString(), new ThumbnailExtractParameters(
                    frameProvider,
                    new MediaCodecDecoder(),
                    new MediaExtractorMediaSource(requireContext(), sourceMedia.uri, new MediaRange(0, Long.MAX_VALUE)),
                    0,
                    new GlThumbnailRenderer(null, listener)
            ));
        } catch (MediaSourceException e) {
            e.printStackTrace();
        }

//        File targetFile = new File(TransformationUtil.getTargetFileDirectory(requireContext().getApplicationContext()),
//                              "transcoded_" + TransformationUtil.getDisplayName(getContext(), sourceMedia.uri));
//        binding.getTargetMedia().setTargetFile(targetFile);
//        binding.getTargetMedia().setTracks(sourceMedia.tracks);
//
//        binding.getTransformationState().setState(TransformationState.STATE_IDLE);
//        binding.getTransformationState().setStats(null);
    }

}
