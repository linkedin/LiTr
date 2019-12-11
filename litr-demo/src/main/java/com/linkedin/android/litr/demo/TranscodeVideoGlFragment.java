/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.TransformationListener;
import com.linkedin.android.litr.analytics.TrackTransformationInfo;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.filter.video.gl.AnimationFrameProvider;
import com.linkedin.android.litr.filter.video.gl.BitmapOverlayFilter;
import com.linkedin.android.litr.filter.video.gl.FrameSequenceAnimationOverlayFilter;
import com.linkedin.android.litr.utils.TrackMetadataUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;

public class TranscodeVideoGlFragment extends Fragment {

    private static final String TAG = TranscodeVideoGlFragment.class.getSimpleName();

    private static final int BYTES_IN_MB = 1024 * 1024;

    private static final int REQUEST_PICK_VIDEO = 42;
    private static final int REQUEST_PICK_VIDEO_OVERLAY = 43;

    private ScrollView scrollableContainer;

    private Button buttonPickVideo;
    private Button buttonPickVideoOverlay;
    private Button buttonTranscode;
    private Button buttonCancel;
    private Button buttonPlay;

    private View sectionSource;
    private View sectionTranscode;
    private View sectionTarget;

    private TextView sourcePath;
    private TextView sourceTrackMetadata;
    private TextView sourceSize;
    private TextView sourceOverlayPath;

    private EditText targetHeight;
    private EditText targetWidth;

    private EditText targetBitrate;
    private EditText targetKeyFrameInterval;
    private EditText targetAudioBitrate;

    private TextView estimatedTargetSize;
    private ProgressBar progressBar;
    private TextView transcodingDuration;

    private TextView targetPath;
    private TextView targetSize;
    private TextView transcodingStats;

    private Switch transcodeAudioSwitch;

    private long startTime;

    private MediaTransformer mediaTransformer;

    private Uri sourceVideoUri;
    private String sourceVideoName;
    private File targetVideoFile;

    private String currentRequestId;

    private Uri overlayUri;

    private TransformationListener videoTransformationListener = new TransformationListener() {
        @Override
        public void onStarted(@NonNull String requestId) {
            currentRequestId = requestId;

            startTime = SystemClock.elapsedRealtime();
            progressBar.setIndeterminate(false);

            buttonCancel.setEnabled(true);
            scrollToBottom();
        }

        @Override
        public void onProgress(@NonNull String requestId, float progress) {
            progressBar.setProgress((int) (progress * MediaTransformer.GRANULARITY_DEFAULT));
        }

        @Override
        public void onCompleted(@NonNull String requestId, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
            currentRequestId = null;

            long totalTime = SystemClock.elapsedRealtime() - startTime;

            progressBar.setProgress(MediaTransformer.GRANULARITY_DEFAULT);
            targetPath.setText(getString(R.string.target_path, targetVideoFile.getAbsolutePath()));
            transcodingDuration.setText(getString(R.string.transformation_duration, totalTime));

            float size = targetVideoFile.length() / BYTES_IN_MB;
            targetSize.setText(getString(R.string.size, size));

            buttonCancel.setEnabled(false);
            transcodingStats.setText(TrackMetadataUtil.printTransformationStats(getContext(),
                                                                                trackTransformationInfos));
            transcodingStats.setVisibility(View.VISIBLE);
            sectionTarget.setVisibility(View.VISIBLE);
            scrollToBottom();
        }

        @Override
        public void onCancelled(@NonNull String requestId, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
            transcodingStats.setText(TrackMetadataUtil.printTransformationStats(getContext(), trackTransformationInfos));
            transcodingStats.setVisibility(View.VISIBLE);

            currentRequestId = null;
            buttonCancel.setEnabled(false);
            scrollToBottom();
            Toast.makeText(getContext(), R.string.cancelled, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(@NonNull String requestId, @Nullable Throwable cause, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
            transcodingStats.setText(TrackMetadataUtil.printTransformationStats(getContext(), trackTransformationInfos));
            transcodingStats.setVisibility(View.VISIBLE);

            currentRequestId = null;
            buttonCancel.setEnabled(false);
            scrollToBottom();

            String errorString = cause == null ? getString(R.string.unknown_error) : cause.getMessage();
            Toast.makeText(getContext(), getString(R.string.transcoding_error, errorString), Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaTransformer = new MediaTransformer(getContext().getApplicationContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transcoding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initViews(view);
        initClickListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaTransformer.release();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_VIDEO) {
                handleVideoPicked(data);
                buttonPickVideoOverlay.setVisibility(View.VISIBLE);
            } else if (requestCode == REQUEST_PICK_VIDEO_OVERLAY) {
                handleOverlayImagePicked(data);
            }
        }
    }

    private void handleVideoPicked(@NonNull Intent data) {
        sourceVideoUri = data.getData();
        if (sourceVideoUri == null) {
            // nothing was selected so don't do nothing
            return;
        }

        sourcePath.setText(getString(R.string.source_path, sourceVideoUri.toString()));
        sourceTrackMetadata.setText(TrackMetadataUtil.printTrackMetadata(getContext(), sourceVideoUri));

        String[] projection = {
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DISPLAY_NAME};
        Cursor cursor = getContext().getContentResolver().query(sourceVideoUri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                long size = cursor.getLong(0);
                float sizeMb = (float) size / BYTES_IN_MB;
                sourceSize.setText(getString(R.string.size, sizeMb));
                sourceVideoName = cursor.getString(1);
            }
            cursor.close();
        }

        sectionSource.setVisibility(View.VISIBLE);
    }

    private void initViews(@NonNull View container) {
        scrollableContainer = container.findViewById(R.id.scrollable_container);

        buttonPickVideo = container.findViewById(R.id.button_pick_video);
        buttonPickVideoOverlay = container.findViewById(R.id.button_pick_video_overlay);
        buttonTranscode = container.findViewById(R.id.button_transcode);
        buttonCancel = container.findViewById(R.id.button_cancel);
        buttonPlay = container.findViewById(R.id.button_play);

        sectionSource = container.findViewById(R.id.section_source);
        sectionTranscode = container.findViewById(R.id.section_transcode);
        sectionTarget = container.findViewById(R.id.section_target);

        sourcePath = container.findViewById(R.id.source_path);
        sourceTrackMetadata = container.findViewById(R.id.source_track_metadata);
        sourceSize = container.findViewById(R.id.source_size);
        sourceOverlayPath = container.findViewById(R.id.source_overlay);

        targetHeight = container.findViewById(R.id.target_height);
        targetWidth = container.findViewById(R.id.target_width);

        targetBitrate = container.findViewById(R.id.target_bitrate);
        targetKeyFrameInterval = container.findViewById(R.id.target_key_frame_interval);
        targetAudioBitrate = container.findViewById(R.id.target_audio_bitrate);

        estimatedTargetSize = container.findViewById(R.id.estimated_target_size);
        progressBar = container.findViewById(R.id.progressBar);
        transcodingDuration = container.findViewById(R.id.transcoding_duration);

        targetPath = container.findViewById(R.id.target_path);
        targetSize = container.findViewById(R.id.target_size);
        transcodingStats = container.findViewById(R.id.transcoding_stats);

        transcodeAudioSwitch = container.findViewById(R.id.target_transcode_audio);
    }

    private void initClickListeners() {
        buttonPickVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickVideo();

                sectionTranscode.setVisibility(View.GONE);
                sectionTarget.setVisibility(View.GONE);

                overlayUri = null;
                sourceOverlayPath.setVisibility(View.GONE);
            }
        });

        buttonPickVideoOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_source_video)),
                                       REQUEST_PICK_VIDEO_OVERLAY);
            }
        });

        targetAudioBitrate.setEnabled(transcodeAudioSwitch.isChecked());
        transcodeAudioSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                targetAudioBitrate.setEnabled(isChecked);
            }
        });

        buttonTranscode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int height = Integer.parseInt(targetHeight.getText().toString());
                int width = Integer.parseInt(targetWidth.getText().toString());
                float bitRateMbps = Float.parseFloat(targetBitrate.getText().toString());
                int bitrate = (int) (bitRateMbps * BYTES_IN_MB);
                int keyFrameInterval = Integer.parseInt(targetKeyFrameInterval.getText().toString());

                File targetDirectory = getTargetFileDirectory();

                targetVideoFile = new File(targetDirectory, "transcoded_" + sourceVideoName);
                if (targetVideoFile.exists()) {
                    targetVideoFile.delete();
                }

                targetPath.setText(getString(R.string.target_path, targetVideoFile.getAbsolutePath()));

                sectionTranscode.setVisibility(View.VISIBLE);
                MediaFormat targetVideoFormat = new MediaFormat();
                targetVideoFormat.setString(MediaFormat.KEY_MIME, "video/avc");
                targetVideoFormat.setInteger(MediaFormat.KEY_WIDTH, width);
                targetVideoFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
                targetVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
                targetVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyFrameInterval);
                targetVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                targetVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

                MediaFormat targetAudioFormat = null;
                if (transcodeAudioSwitch.isChecked()) {
                    int targetAudioBitrateKbps = Integer.parseInt(targetAudioBitrate.getText().toString());
                    targetAudioFormat = createTargetAudioFormat(targetAudioBitrateKbps);
                }

                long estimatedTranscodedSize = mediaTransformer.getEstimatedTargetVideoSize(sourceVideoUri,
                                                                                            targetVideoFormat,
                                                                                            targetAudioFormat);
                estimatedTargetSize.setText(getString(R.string.estimated_target_size,
                                                      (float) estimatedTranscodedSize / BYTES_IN_MB));

                String requestId = UUID.randomUUID().toString();

                List<GlFilter> glFilters = null;
                if (overlayUri != null) {
                    try {
                        Context context = getContext();
                        Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(overlayUri));
                        if (bitmap != null) {
                            int videoRotation = getVideoRotation();

                            float overlayWidth = 0.56f;
                            float overlayHeight;
                            if (videoRotation == 90 || videoRotation == 270) {
                                float overlayWidthPixels = overlayWidth * height;
                                float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                                overlayHeight = overlayHeightPixels / width;
                            } else {
                                float overlayWidthPixels = overlayWidth * width;
                                float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                                overlayHeight = overlayHeightPixels / height;
                            }

                            PointF position = new PointF(0.6f, 0.4f);
                            PointF size = new PointF(overlayWidth, overlayHeight);
                            float rotation = 30;

                            if (TextUtils.equals(context.getContentResolver().getType(overlayUri), "image/gif")) {
                                ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
                                InputStream inputStream = contentResolver.openInputStream(overlayUri);
                                BitmapPool bitmapPool = new LruBitmapPool(10);
                                GifBitmapProvider gifBitmapProvider = new GifBitmapProvider(bitmapPool);
                                final GifDecoder gifDecoder = new StandardGifDecoder(gifBitmapProvider);
                                gifDecoder.read(inputStream, (int) getGifSize(overlayUri));

                                AnimationFrameProvider animationFrameProvider = new AnimationFrameProvider() {
                                    @Override
                                    public int getFrameCount() {
                                        return gifDecoder.getFrameCount();
                                    }

                                    @Nullable
                                    @Override
                                    public Bitmap getNextFrame() {
                                        return gifDecoder.getNextFrame();
                                    }

                                    @Override
                                    public long getNextFrameDurationNs() {
                                        return TimeUnit.MILLISECONDS.toNanos(gifDecoder.getNextDelay());
                                    }

                                    @Override
                                    public void advance() {
                                        gifDecoder.advance();
                                    }
                                };

                                GlFilter animatedGifOverlayFilter = new FrameSequenceAnimationOverlayFilter(animationFrameProvider, size, position, rotation);
                                glFilters = Collections.singletonList(animatedGifOverlayFilter);
                            } else {
                                GlFilter bitmapOverlayFilter = new BitmapOverlayFilter(context.getApplicationContext(), overlayUri, size, position, rotation);
                                glFilters = Collections.singletonList(bitmapOverlayFilter);
                            }
                            bitmap.recycle();
                        }
                    } catch (IOException ex) {
                        Log.e(TAG, "Failed to extract bitmap size");
                    }
                }

                mediaTransformer.transform(requestId,
                                           sourceVideoUri,
                                           targetVideoFile.getAbsolutePath(),
                                           targetVideoFormat,
                                           targetAudioFormat,
                                           videoTransformationListener,
                                           MediaTransformer.GRANULARITY_DEFAULT,
                                           glFilters);

            }
        });

        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent playIntent = new Intent(Intent.ACTION_VIEW);
                Uri videoUri = FileProvider.getUriForFile(getContext(),
                                                          getContext().getPackageName() + ".provider",
                                                          new File(targetVideoFile.getAbsolutePath()));
                playIntent.setDataAndType(videoUri, "video/*");
                playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(playIntent);
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaTransformer.cancel(currentRequestId);
            }
        });
    }

    private void handleOverlayImagePicked(@NonNull Intent data) {
        overlayUri = data.getData();
        if (overlayUri != null) {
            sourceOverlayPath.setText(getString(R.string.source_path, overlayUri.toString()));
            sourceOverlayPath.setVisibility(View.VISIBLE);
        }
    }

    private void scrollToBottom() {
        scrollableContainer.post(new Runnable() {
            @Override
            public void run() {
                scrollableContainer.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void pickVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_source_video)),
                               REQUEST_PICK_VIDEO);
    }

    @NonNull
    private File getTargetFileDirectory() {
        File targetDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                                            + File.separator
                                            + "LiTr");
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        return targetDirectory;
    }



    @Nullable
    private MediaFormat createTargetAudioFormat(int targetBitrateKbps) {
        MediaFormat audioMediaFormat = null;
        try {
            MediaExtractor mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(getContext(), sourceVideoUri, null);
            int trackCount = mediaExtractor.getTrackCount();
            for (int track = 0; track < trackCount; track++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(track);
                String mimeType = null;
                if (mediaFormat.containsKey(MediaFormat.KEY_MIME)) {
                    mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                }
                if (mimeType != null && mimeType.startsWith("audio")) {
                    audioMediaFormat = new MediaFormat();
                    audioMediaFormat.setString(MediaFormat.KEY_MIME, mediaFormat.getString(MediaFormat.KEY_MIME));
                    audioMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                    audioMediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                    audioMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, targetBitrateKbps * 1000);
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to extract audio track metadata: " + ex);
        }

        return audioMediaFormat;
    }

    private long getGifSize(@NonNull Uri uri) {
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            AssetFileDescriptor fileDescriptor = null;
            try {
                fileDescriptor = getContext().getContentResolver().openAssetFileDescriptor(uri, "r");
                long size = fileDescriptor != null ? fileDescriptor.getParcelFileDescriptor().getStatSize() : 0;
                return size < 0 ? 0 : size;
            } catch (FileNotFoundException | IllegalStateException e) {
                Log.e(TAG, "Unable to extract length from uri: " + uri, e);
                return 0;
            } finally {
                if (fileDescriptor != null) {
                    try {
                        fileDescriptor.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to close file descriptor from uri: " + uri, e);
                    }
                }
            }
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme()) && uri.getPath() != null) {
            File file = new File(uri.getPath());
            return file.length();
        } else {
            return 0;
        }
    }

    private int getVideoRotation() {
        int rotation = 0;
        try {
            MediaExtractor mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(getContext(), sourceVideoUri, null);
            int trackCount = mediaExtractor.getTrackCount();
            for (int track = 0; track < trackCount; track++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(track);
                if (mediaFormat.containsKey(MediaFormat.KEY_MIME)
                    && mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("video")
                    && mediaFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                        rotation = mediaFormat.getInteger(MediaFormat.KEY_ROTATION);
                    }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to extract audio track metadata: " + ex);
        }

        return rotation;
    }
}
