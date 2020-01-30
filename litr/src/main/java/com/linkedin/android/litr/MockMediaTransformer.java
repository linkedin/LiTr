package com.linkedin.android.litr;

import android.media.MediaFormat;
import android.net.Uri;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.Renderer;

import java.util.List;

public class MockMediaTransformer extends MediaTransformer {

    @Override
    public void transform(@NonNull String requestId,
                          @NonNull Uri inputUri,
                          @NonNull String outputFilePath,
                          @Nullable MediaFormat targetVideoFormat,
                          @Nullable MediaFormat targetAudioFormat,
                          @NonNull TransformationListener listener,
                          @IntRange(from = GRANULARITY_NONE) int granularity,
                          @Nullable List<GlFilter> filters) {
    }

    @Override
    public void transform(@NonNull String requestId,
                          @NonNull MediaSource mediaSource,
                          @NonNull Decoder decoder,
                          @NonNull Renderer videoRenderer,
                          @NonNull Encoder encoder,
                          @NonNull MediaTarget mediaTarget,
                          @Nullable MediaFormat targetVideoFormat,
                          @Nullable MediaFormat targetAudioFormat,
                          @NonNull TransformationListener listener,
                          @IntRange(from = GRANULARITY_NONE) int granularity) {
    }

    @Override
    public void transform(@NonNull String requestId,
                          List<TrackTransform> trackTransforms,
                          @NonNull TransformationListener listener,
                          @IntRange(from = GRANULARITY_NONE) int granularity) {
    }
}
