/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.demo.BaseTransformationFragment;
import com.linkedin.android.litr.demo.MediaPickerListener;
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.demo.data.SquareCenterCropPresenter;
import com.linkedin.android.litr.demo.data.TargetMedia;
import com.linkedin.android.litr.demo.data.TransformationState;
import com.linkedin.android.litr.demo.databinding.FragmentSquareCenterCropBinding;
import com.linkedin.android.litr.utils.TransformationUtil;

import java.io.File;

public class SquareCenterCropFragment extends BaseTransformationFragment implements MediaPickerListener {

    private FragmentSquareCenterCropBinding binding;

    private MediaTransformer mediaTransformer;
    private TargetMedia targetMedia;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaTransformer = new MediaTransformer(getContext().getApplicationContext());
        targetMedia = new TargetMedia();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaTransformer.release();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSquareCenterCropBinding.inflate(inflater, container, false);

        SourceMedia sourceMedia = new SourceMedia();
        binding.setSourceMedia(sourceMedia);

        binding.sectionPickVideo.buttonPickVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickVideo(SquareCenterCropFragment.this);
            }
        });

        binding.setTransformationState(new TransformationState());
        binding.setTransformationPresenter(new SquareCenterCropPresenter(getContext(), mediaTransformer));

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
