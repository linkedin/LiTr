/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.MimeType;
import com.linkedin.android.litr.TrackTransform;
import com.linkedin.android.litr.TransformationOptions;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.codec.MediaCodecDecoder;
import com.linkedin.android.litr.codec.MediaCodecEncoder;
import com.linkedin.android.litr.codec.PassthroughBufferEncoder;
import com.linkedin.android.litr.codec.PassthroughDecoder;
import com.linkedin.android.litr.demo.R;
import com.linkedin.android.litr.exception.MediaTransformationException;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.filter.video.gl.SolidBackgroundColorFilter;
import com.linkedin.android.litr.io.AudioRecordMediaSource;
import com.linkedin.android.litr.io.MediaExtractorMediaSource;
import com.linkedin.android.litr.io.MediaMuxerMediaTarget;
import com.linkedin.android.litr.io.MediaRange;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.io.MockVideoMediaSource;
import com.linkedin.android.litr.io.WavMediaTarget;
import com.linkedin.android.litr.render.AudioRenderer;
import com.linkedin.android.litr.render.GlVideoRenderer;
import com.linkedin.android.litr.utils.TransformationUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TransformationPresenter {

    private static final String TAG = TransformationPresenter.class.getSimpleName();

    protected static final String KEY_ROTATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? MediaFormat.KEY_ROTATION
            : "rotation-degrees";

    private final Context context;
    private final MediaTransformer mediaTransformer;

    public TransformationPresenter(@NonNull Context context,
                                   @NonNull MediaTransformer mediaTransformer) {
        this.context = context;
        this.mediaTransformer = mediaTransformer;
    }

    public void applyFilter(@NonNull SourceMedia sourceMedia,
                            @NonNull TargetMedia targetMedia,
                            @NonNull TransformationState transformationState) {
        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete();
        }

        transformationState.requestId = UUID.randomUUID().toString();
        MediaTransformationListener transformationListener = new MediaTransformationListener(context,
                transformationState.requestId,
                transformationState,
                targetMedia);

        TransformationOptions transformationOptions = new TransformationOptions.Builder()
                .setGranularity(MediaTransformer.GRANULARITY_DEFAULT)
                .setVideoFilters(Collections.singletonList(targetMedia.filter))
                .build();

        mediaTransformer.transform(
                transformationState.requestId,
                sourceMedia.uri,
                targetMedia.targetFile.getPath(),
                null,
                null,
                transformationListener,
                transformationOptions);
    }

    public void transcodeAudio(@NonNull SourceMedia sourceMedia,
                               @NonNull TargetMedia targetMedia,
                               @NonNull TrimConfig trimConfig,
                               @NonNull TransformationState transformationState) {
        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete();
        }

        transformationState.requestId = UUID.randomUUID().toString();
        MediaTransformationListener transformationListener = new MediaTransformationListener(context,
                transformationState.requestId,
                transformationState,
                targetMedia);

        MediaRange mediaRange = trimConfig.enabled
                ? new MediaRange(
                TimeUnit.MILLISECONDS.toMicros((long) (trimConfig.range.get(0) * 1000)),
                TimeUnit.MILLISECONDS.toMicros((long) (trimConfig.range.get(1) * 1000)))
                : new MediaRange(0, Long.MAX_VALUE);

        try {
            String targetMimeType = targetMedia.writeToWav ? "audio/raw" : "audio/mp4a-latm";
            MediaTarget mediaTarget = targetMedia.writeToWav
                    ? new WavMediaTarget(targetMedia.targetFile.getPath())
                    : new MediaMuxerMediaTarget(targetMedia.targetFile.getPath(), 1, 0, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaSource mediaSource = new MediaExtractorMediaSource(context, sourceMedia.uri, mediaRange);
            List<TrackTransform> trackTransforms = new ArrayList<>(1);

            for (TargetTrack targetTrack : targetMedia.tracks) {
                if (targetTrack.format instanceof AudioTrackFormat) {
                    AudioTrackFormat trackFormat = (AudioTrackFormat) targetTrack.format;
                    MediaFormat mediaFormat = MediaFormat.createAudioFormat(
                            targetMimeType,
                            trackFormat.samplingRate,
                            trackFormat.channelCount);
                    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
                    mediaFormat.setLong(MediaFormat.KEY_DURATION, trackFormat.duration);

                    Encoder encoder = targetMedia.writeToWav ? new PassthroughBufferEncoder(8192) : new MediaCodecEncoder();
                    TrackTransform trackTransform = new TrackTransform.Builder(mediaSource, targetTrack.sourceTrackIndex, mediaTarget)
                            .setTargetTrack(0)
                            .setDecoder(new MediaCodecDecoder())
                            .setEncoder(encoder)
                            .setRenderer(new AudioRenderer(encoder))
                            .setTargetFormat(mediaFormat)
                            .build();

                    trackTransforms.add(trackTransform);
                    break;
                }
            }

            mediaTransformer.transform(
                    transformationState.requestId,
                    trackTransforms,
                    transformationListener,
                    MediaTransformer.GRANULARITY_DEFAULT);
        } catch (MediaTransformationException ex) {
            Log.e(TAG, "Exception when trying to transcode audio", ex);
        }
    }

    public void transcodeToVp9(@NonNull SourceMedia sourceMedia,
                               @NonNull TargetMedia targetMedia,
                               @NonNull TrimConfig trimConfig,
                               @NonNull TransformationState transformationState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(context, R.string.error_vp9_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete();
        }

        transformationState.requestId = UUID.randomUUID().toString();
        MediaTransformationListener transformationListener = new MediaTransformationListener(context,
                transformationState.requestId,
                transformationState,
                targetMedia);

        MediaRange mediaRange = trimConfig.enabled
                ? new MediaRange(
                TimeUnit.MILLISECONDS.toMicros((long) (trimConfig.range.get(0) * 1000)),
                TimeUnit.MILLISECONDS.toMicros((long) (trimConfig.range.get(1) * 1000)))
                : new MediaRange(0, Long.MAX_VALUE);
        TransformationOptions transformationOptions = new TransformationOptions.Builder()
                .setGranularity(MediaTransformer.GRANULARITY_DEFAULT)
                .setSourceMediaRange(mediaRange)
                .setRemoveMetadata(true)
                .build();

        MediaFormat targetVideoFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_VP9,
                1280,
                720);
        targetVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 5_000_000);
        targetVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        targetVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

        mediaTransformer.transform(
                transformationState.requestId,
                sourceMedia.uri,
                targetMedia.targetFile.getPath(),
                targetVideoFormat,
                null,
                transformationListener,
                transformationOptions);
    }

    public void recordAudio(@NonNull AudioRecordMediaSource mediaSource,
                            @NonNull TargetMedia targetMedia,
                            @NonNull TransformationState transformationState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw new UnsupportedOperationException("Android Marshmallow or newer required");
        }

        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete();
        }

        transformationState.requestId = UUID.randomUUID().toString();
        MediaTransformationListener transformationListener = new MediaTransformationListener(context,
                transformationState.requestId,
                transformationState,
                targetMedia);

        try {
            MediaTarget mediaTarget = new MediaMuxerMediaTarget(targetMedia.targetFile.getPath(),
                    2,
                    0,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // Create a single (synthetic) video track to ensure our output is playable by the demo
            // app. We only use 1 second of video (a solid color) so the duration of the video will
            // likely depend on the length of the audio recording.
            VideoTrackFormat videoTrackFormat = new VideoTrackFormat(0, MimeType.VIDEO_AVC);
            videoTrackFormat.duration = TimeUnit.SECONDS.toMicros(1);
            videoTrackFormat.frameRate = 30;
            videoTrackFormat.width = 512;
            videoTrackFormat.height = 512;

            MediaFormat videoMediaFormat = createVideoMediaFormat(videoTrackFormat);
            MediaSource videoMediaSource =
                    new MockVideoMediaSource(videoMediaFormat);

            SolidBackgroundColorFilter filter = new SolidBackgroundColorFilter(Color.RED);

            TrackTransform.Builder videoTransformBuilder = new TrackTransform.Builder(
                    videoMediaSource,
                    0,
                    mediaTarget)
                    .setTargetTrack(0)
                    .setTargetFormat(videoMediaFormat)
                    .setEncoder(new MediaCodecEncoder())
                    .setDecoder(new PassthroughDecoder(1))
                    .setRenderer(new GlVideoRenderer(Collections.singletonList(filter)));

            AudioTrackFormat audioTrackFormat = new AudioTrackFormat(0, MimeType.AUDIO_AAC);
            audioTrackFormat.samplingRate = 44100;
            audioTrackFormat.channelCount = 1;
            audioTrackFormat.bitrate = 64 * 1024;

            TrackTransform.Builder audioTransformBuilder = new TrackTransform.Builder(mediaSource,
                    0,
                    mediaTarget)
                    .setTargetTrack(1)
                    .setTargetFormat(createAudioMediaFormat(audioTrackFormat))
                    .setEncoder(new MediaCodecEncoder())
                    .setDecoder(new MediaCodecDecoder());

            ArrayList<TrackTransform> trackTransforms = new ArrayList<>();
            trackTransforms.add(videoTransformBuilder.build());
            trackTransforms.add(audioTransformBuilder.build());

            mediaSource.start();
            mediaTransformer.transform(
                    transformationState.requestId,
                    trackTransforms,
                    transformationListener,
                    MediaTransformer.GRANULARITY_DEFAULT);
        } catch (MediaTransformationException ex) {
            Log.e(TAG, "Exception when trying to perform track operation", ex);
        }
    }

    public void stopRecording(@NonNull AudioRecordMediaSource mediaSource) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw new UnsupportedOperationException("Android Marshmallow or newer required");
        }

        mediaSource.stop();
    }

    public void cancelTransformation(@NonNull String requestId) {
        mediaTransformer.cancel(requestId);
    }

    public void play(@Nullable Uri contentUri) {
        if (contentUri != null) {
            Intent playIntent = new Intent(Intent.ACTION_VIEW);
            playIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
            playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(playIntent);
        }
    }

    @Nullable
    protected List<GlFilter> createGlFilters(@NonNull SourceMedia sourceMedia,
                                           @Nullable TargetVideoTrack targetTrack,
                                           float overlayWidth,
                                           @NonNull PointF position,
                                           float rotation) {
        List<GlFilter> glFilters = null;
        if (targetTrack != null && targetTrack.overlay != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(targetTrack.overlay));
                if (bitmap != null) {
                    float overlayHeight;
                    VideoTrackFormat sourceVideoTrackFormat = (VideoTrackFormat) sourceMedia.tracks.get(targetTrack.sourceTrackIndex);
                    if (sourceVideoTrackFormat.rotation == 90 || sourceVideoTrackFormat.rotation == 270) {
                        float overlayWidthPixels = overlayWidth * sourceVideoTrackFormat.height;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / sourceVideoTrackFormat.width;
                    } else {
                        float overlayWidthPixels = overlayWidth * sourceVideoTrackFormat.width;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / sourceVideoTrackFormat.height;
                    }

                    PointF size = new PointF(overlayWidth, overlayHeight);

                    GlFilter filter = TransformationUtil.createGlFilter(context,
                                                                        targetTrack.overlay,
                                                                        size,
                                                                        position,
                                                                        rotation);
                    if (filter != null) {
                        glFilters = new ArrayList<>();
                        glFilters.add(filter);
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG, "Failed to extract audio track metadata: " + ex);
            }
        }

        return glFilters;
    }

    @Nullable
    protected MediaFormat createMediaFormat(@Nullable TargetTrack targetTrack) {
        MediaFormat mediaFormat = null;
        if (targetTrack != null && targetTrack.format != null) {
            if (targetTrack.format.mimeType.startsWith("video")) {
                mediaFormat = createVideoMediaFormat((VideoTrackFormat) targetTrack.format);
            } else if (targetTrack.format.mimeType.startsWith("audio")) {
                mediaFormat = createAudioMediaFormat((AudioTrackFormat) targetTrack.format);
            }
        }

        return mediaFormat;
    }

    @NonNull
    protected MediaFormat createVideoMediaFormat(@NonNull VideoTrackFormat trackFormat) {
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, trackFormat.mimeType);
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, trackFormat.width);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, trackFormat.height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, trackFormat.keyFrameInterval);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, trackFormat.frameRate);
        mediaFormat.setLong(MediaFormat.KEY_DURATION, trackFormat.duration);
        mediaFormat.setInteger(KEY_ROTATION, trackFormat.rotation);

        return mediaFormat;
    }

    @NonNull
    protected MediaFormat createAudioMediaFormat(@NonNull AudioTrackFormat trackFormat) {
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, trackFormat.mimeType);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, trackFormat.channelCount);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, trackFormat.samplingRate);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
        mediaFormat.setLong(MediaFormat.KEY_DURATION, trackFormat.duration);

        return mediaFormat;
    }

    protected boolean hasVp8OrVp9Track(@NonNull List<TargetTrack> targetTracks) {
        for (TargetTrack targetTrack : targetTracks) {
            if (!targetTrack.shouldInclude) {
                continue;
            }

            if (TextUtils.equals(targetTrack.format.mimeType, MimeType.VIDEO_VP8)
                    || TextUtils.equals(targetTrack.format.mimeType, MimeType.VIDEO_VP9)) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasVp8OrVp9TrackFormat(@NonNull List<MediaTrackFormat> trackFormats) {
        for (MediaTrackFormat trackFormat : trackFormats) {
            if (TextUtils.equals(trackFormat.mimeType, MimeType.VIDEO_VP8)
                    || TextUtils.equals(trackFormat.mimeType, MimeType.VIDEO_VP9)) {
                return true;
            }
        }
        return false;
    }
}
