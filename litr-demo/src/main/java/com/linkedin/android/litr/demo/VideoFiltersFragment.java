/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.demo.data.TargetMedia;
import com.linkedin.android.litr.demo.data.TransformationPresenter;
import com.linkedin.android.litr.demo.data.TransformationState;
import com.linkedin.android.litr.demo.databinding.FragmentVideoFiltersBinding;
import com.linkedin.android.litr.utils.TransformationUtil;

import java.io.File;

public class VideoFiltersFragment extends BaseTransformationFragment implements MediaPickerListener {

    private FragmentVideoFiltersBinding binding;

    private MediaTransformer mediaTransformer;
    private TargetMedia targetMedia;

    private ArrayAdapter<DemoFilter> adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaTransformer = new MediaTransformer(getContext().getApplicationContext());
        targetMedia = new TargetMedia();

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, DemoFilter.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        File targetFile = new File(TransformationUtil.getTargetFileDirectory(requireContext().getApplicationContext()),
                              "transcoded_" + TransformationUtil.getDisplayName(getContext(), sourceMedia.uri));
        binding.getTargetMedia().setTargetFile(targetFile);
        binding.getTargetMedia().setTracks(sourceMedia.tracks);

        binding.getTransformationState().setState(TransformationState.STATE_IDLE);
        binding.getTransformationState().setStats(null);
    }

}
