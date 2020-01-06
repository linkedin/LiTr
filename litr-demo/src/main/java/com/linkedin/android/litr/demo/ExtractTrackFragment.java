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
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.demo.data.TransformationPresenter;
import com.linkedin.android.litr.demo.data.TransformationState;
import com.linkedin.android.litr.demo.data.VideoTarget;
import com.linkedin.android.litr.demo.databinding.FragmentExtractTrackBinding;

public class ExtractTrackFragment extends Fragment implements MediaPickerTarget {

    private MediaTransformer mediaTransformer;

    private FragmentExtractTrackBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaTransformer = new MediaTransformer(getContext().getApplicationContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentExtractTrackBinding.inflate(inflater, container, false);

        binding.sectionPickVideo.buttonPickVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickMedia(MediaPickerFragment.PICK_VIDEO);
            }
        });

        VideoTarget videoTarget = new VideoTarget();
        videoTarget.shouldKeepTrack = true;
        binding.setVideoTarget(videoTarget);
        binding.setAudioTarget(new AudioTarget());
        binding.setTransformationState(new TransformationState());
        binding.setPresenter(new TransformationPresenter(getContext(), mediaTransformer));

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaTransformer.release();
    }

    @Override
    public void onMediaPicked(@NonNull SourceMedia sourceMedia) {
        binding.setSourceMedia(sourceMedia);
        binding.getTransformationState().setState(TransformationState.STATE_IDLE);
        binding.getTransformationState().setStats(null);
    }

    @Override
    public void onOverlayPicked(@NonNull Uri uri, long size) {}

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
