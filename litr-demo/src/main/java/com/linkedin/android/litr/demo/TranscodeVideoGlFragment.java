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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.demo.data.AudioTarget;
import com.linkedin.android.litr.demo.data.OverlayTarget;
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.demo.data.TransformationPresenter;
import com.linkedin.android.litr.demo.data.TransformationState;
import com.linkedin.android.litr.demo.data.VideoTarget;
import com.linkedin.android.litr.demo.databinding.FragmentTranscodeVideoGlBinding;

public class TranscodeVideoGlFragment extends Fragment implements MediaPickerTarget {

    private FragmentTranscodeVideoGlBinding binding;

    private MediaTransformer mediaTransformer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaTransformer = new MediaTransformer(getContext().getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaTransformer.release();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTranscodeVideoGlBinding.inflate(inflater, container, false);

        binding.sectionPickVideo.buttonPickVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickMedia(MediaPickerFragment.PICK_VIDEO);
            }
        });

        binding.sectionPickOverlay.buttonPickVideoOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickMedia(MediaPickerFragment.PICK_OVERLAY);
            }
        });

        binding.setVideoTarget(new VideoTarget());
        binding.setAudioTarget(new AudioTarget());
        binding.setOverlayTarget(new OverlayTarget());
        binding.setTransformationState(new TransformationState());
        binding.setPresenter(new TransformationPresenter(getContext(), mediaTransformer));

        return binding.getRoot();
    }

    @Override
    public void onMediaPicked(@NonNull SourceMedia sourceMedia) {
        binding.setSourceMedia(sourceMedia);
        binding.getTransformationState().setState(TransformationState.STATE_IDLE);
        binding.getTransformationState().setStats(null);
    }

    @Override
    public void onOverlayPicked(@NonNull Uri uri, long size) {
        OverlayTarget overlayTarget = binding.getOverlayTarget();
        overlayTarget.setUri(uri);
        overlayTarget.setSize(size);
    }

    private void pickMedia(int mediaType) {
        MediaPickerFragment mediaPickerFragment = new MediaPickerFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(MediaPickerFragment.KEY_PICK_TYPE, mediaType);
        mediaPickerFragment.setArguments(arguments);
        mediaPickerFragment.setTargetFragment(this, mediaType);

        getActivity().getSupportFragmentManager().beginTransaction()
                     .add(mediaPickerFragment, "MediaPickerFragment")
                     .addToBackStack(null)
                     .commit();
    }

}
