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
import android.media.MediaMetadataRetriever;
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

import com.linkedin.android.litr.codec.MediaCodecDecoder;
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.demo.databinding.FragmentExtractFramesBinding;
import com.linkedin.android.litr.exception.MediaSourceException;
import com.linkedin.android.litr.filter.GlFilter;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class ExtractFramesFragment extends BaseTransformationFragment implements MediaPickerListener {

    private FragmentExtractFramesBinding binding;

    private ArrayAdapter<DemoFilter> filtersAdapter;

    private VideoThumbnailExtractor thumbnailExtractor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filtersAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, DemoFilter.values());
        filtersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        thumbnailExtractor = new VideoThumbnailExtractor(
                requireContext().getApplicationContext(),
                Executors.newSingleThreadExecutor(),
                new ThumbnailExtractListener() {
                    @Override
                    public void onStarted(@NonNull String id, @NonNull List<Long> timestampsUs) {
                        List<Bitmap> bitmaps = new ArrayList<>(timestampsUs.size());
                        for (int i = 0; i < timestampsUs.size(); i++) {
                            bitmaps.add(null);
                        }
                        binding.filmStripTimeline.setFrameList(bitmaps);
                    }

                    @Override
                    public void onExtracted(@NotNull String id, int index, @Nullable Bitmap bitmap) {
                        binding.filmStripTimeline.setFrameAt(index, bitmap);
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
        thumbnailExtractor.release();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentExtractFramesBinding.inflate(inflater, container, false);

        binding.setFragment(this);

        SourceMedia sourceMedia = new SourceMedia();
        binding.setSourceMedia(sourceMedia);

        binding.sectionPickVideo.buttonPickVideo.setOnClickListener(view -> pickVideo(ExtractFramesFragment.this));

        binding.spinnerFilters.setAdapter(filtersAdapter);
        binding.spinnerFilters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                binding.setFilter(filtersAdapter.getItem(position).filter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return binding.getRoot();
    }



    public void extractThumbnails(@NonNull SourceMedia sourceMedia, @Nullable GlFilter filter) {

        Point sourceSize = TranscoderUtils.getVideoDimensions(requireContext().getApplicationContext(), sourceMedia.uri);
        if (sourceSize == null) {
            return;
        }

        double durationSec = sourceMedia.duration;
        int timelineWidth = binding.filmStripTimeline.getWidth();
        int thumbHeight = binding.filmStripTimeline.getHeight();

        if (durationSec <= 0 || timelineWidth <= 0 || thumbHeight <= 0) {
            return;
        }
        int thumbCount = 10;
        double secPerThumbnail = durationSec / thumbCount;

        List<Long> timestamps = new ArrayList<>();
        for (int i = 0; i < thumbCount; i++) {
            long timestampUs = (long)(secPerThumbnail * 1_000_000.0) * i;
            timestamps.add(timestampUs);
        }

        MediaSourceFactory mediaSourceFactory = new MediaSourceFactory() {
            @NonNull
            @Override
            public MediaSource createMediaSource() throws MediaSourceException {
                return new MediaExtractorMediaSource(
                        requireContext().getApplicationContext(),
                sourceMedia.uri,
                        new MediaRange(0, Long.MAX_VALUE));
            }

            @NonNull
            @Override
            public MediaMetadataRetriever getRetriever() {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(requireContext(), sourceMedia.uri);
                return retriever;
            }
        };

        int thumbWidth = timelineWidth / thumbCount;

        Point destSize = new Point(thumbWidth, thumbHeight);

        thumbnailExtractor.extract(
                UUID.randomUUID().toString(), new ThumbnailExtractParameters(
                        mediaSourceFactory,
                        timestamps,
                        new MediaRange(0, Long.MAX_VALUE),
                        new MediaCodecDecoder(),
                        new MediaCodecDecoder(),
                        sourceSize,
                        destSize,
                        new GlThumbnailRenderer(Collections.singletonList(filter))
                )
        );
    }

    @Override
    public void onMediaPicked(@NonNull Uri uri) {
        SourceMedia sourceMedia = binding.getSourceMedia();
        updateSourceMedia(sourceMedia, uri);
    }

}
