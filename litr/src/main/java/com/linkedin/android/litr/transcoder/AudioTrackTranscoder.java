/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.transcoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.codec.Frame;
import com.linkedin.android.litr.exception.TrackTranscoderException;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.Renderer;

import java.util.concurrent.TimeUnit;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AudioTrackTranscoder extends TrackTranscoder {
    private static final String TAG = AudioTrackTranscoder.class.getSimpleName();

    @VisibleForTesting int lastExtractFrameResult;
    @VisibleForTesting int lastDecodeFrameResult;
    @VisibleForTesting int lastEncodeFrameResult;

    @NonNull private MediaFormat sourceAudioFormat;

    AudioTrackTranscoder(@NonNull MediaSource mediaSource,
                         int sourceTrack,
                         @NonNull MediaTarget mediaTarget,
                         int targetTrack,
                         @NonNull MediaFormat targetFormat,
                         @NonNull Renderer renderer,
                         @NonNull Decoder decoder,
                         @NonNull Encoder encoder) throws TrackTranscoderException {
        super(mediaSource, sourceTrack, mediaTarget, targetTrack, targetFormat, renderer, decoder, encoder);

        lastExtractFrameResult = RESULT_FRAME_PROCESSED;
        lastDecodeFrameResult = RESULT_FRAME_PROCESSED;
        lastEncodeFrameResult = RESULT_FRAME_PROCESSED;

        initCodecs();
    }

    private void initCodecs() throws TrackTranscoderException {
        // create anc configure the encoder

        sourceAudioFormat = mediaSource.getTrackFormat(sourceTrack);

        encoder.init(targetFormat);
        renderer.init(null, sourceAudioFormat, targetFormat);
        decoder.init(sourceAudioFormat, null);
    }

    @Override
    public void start() throws TrackTranscoderException {
        mediaSource.selectTrack(sourceTrack);

        encoder.start();
        decoder.start();
    }

    @Override
    public int processNextFrame() throws TrackTranscoderException {
        if (!encoder.isRunning() || !decoder.isRunning()) {
            // can't do any work
            return ERROR_TRANSCODER_NOT_RUNNING;
        }
        int result = RESULT_FRAME_PROCESSED;

        // extract the frame from the incoming stream and send it to the decoder
        if (lastExtractFrameResult != RESULT_EOS_REACHED) {
            lastExtractFrameResult = extractAndEnqueueInputFrame();
        }

        // receive the decoded frame and send it to the encoder
        if (lastDecodeFrameResult != RESULT_EOS_REACHED) {
            lastDecodeFrameResult = queueDecodedInputFrame();
        }

        // get the encoded frame and write it into the target file
        if (lastEncodeFrameResult != RESULT_EOS_REACHED) {
            lastEncodeFrameResult = writeEncodedOutputFrame();
        }

        if (lastEncodeFrameResult == RESULT_OUTPUT_MEDIA_FORMAT_CHANGED) {
            result = RESULT_OUTPUT_MEDIA_FORMAT_CHANGED;
        }

        if (lastExtractFrameResult == RESULT_EOS_REACHED
            && lastDecodeFrameResult == RESULT_EOS_REACHED
            && lastEncodeFrameResult == RESULT_EOS_REACHED) {
            result = RESULT_EOS_REACHED;
        }

        return result;
    }

    @Override
    public void stop() {
        encoder.stop();
        encoder.release();

        decoder.stop();
        decoder.release();
    }

    private int extractAndEnqueueInputFrame() throws TrackTranscoderException {
        int extractFrameResult = RESULT_FRAME_PROCESSED;

        int selectedTrack = mediaSource.getSampleTrackIndex();
        if (selectedTrack == sourceTrack || selectedTrack == NO_SELECTED_TRACK) {
            int tag = decoder.dequeueInputFrame(0);
            if (tag >= 0) {
                Frame frame = decoder.getInputFrame(tag);
                if (frame == null) {
                    throw new TrackTranscoderException(TrackTranscoderException.Error.NO_FRAME_AVAILABLE);
                }
                int bytesRead = mediaSource.readSampleData(frame.buffer, 0);
                long sampleTime = mediaSource.getSampleTime();
                int sampleFlags = mediaSource.getSampleFlags();

                if (bytesRead < 0 || (sampleFlags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    frame.bufferInfo.set(0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    decoder.queueInputFrame(frame);
                    extractFrameResult = RESULT_EOS_REACHED;
                    Log.d(TAG, "EoS reached on the input stream");
                } else if (sampleTime >= sourceMediaSelection.getEnd()) {
                    frame.bufferInfo.set(0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    decoder.queueInputFrame(frame);
                    advanceToNextTrack();
                    extractFrameResult = RESULT_EOS_REACHED;
                    Log.d(TAG, "Selection end reached on the input stream");
                } else {
                    frame.bufferInfo.set(0, bytesRead, sampleTime, sampleFlags);
                    decoder.queueInputFrame(frame);
                    mediaSource.advance();
                    //Log.d(TAG, "Sample time: " + sampleTime + ", source bytes read: " + bytesRead);
                }
            } else {
                switch (tag) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        //Log.d(TAG, "Will try getting decoder input buffer later");
                        break;
                    default:
                        Log.e(TAG, "Unhandled value " + tag + " when decoding an input frame");
                        break;
                }
            }
        }

        return extractFrameResult;
    }

    private int queueDecodedInputFrame() throws TrackTranscoderException {
        int decodeFrameResult = RESULT_FRAME_PROCESSED;

        int tag = decoder.dequeueOutputFrame(0);
        if (tag >= 0) {
            Frame decoderOutputFrame = decoder.getOutputFrame(tag);
            if (decoderOutputFrame == null) {
                throw new TrackTranscoderException(TrackTranscoderException.Error.NO_FRAME_AVAILABLE);
            }

            if (decoderOutputFrame.bufferInfo.presentationTimeUs >= sourceMediaSelection.getStart()
                    || (decoderOutputFrame.bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                renderer.renderFrame(decoderOutputFrame,
                        TimeUnit.MICROSECONDS.toNanos(decoderOutputFrame.bufferInfo.presentationTimeUs - sourceMediaSelection.getStart()));
            }
            decoder.releaseOutputFrame(tag, false);

            if ((decoderOutputFrame.bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "EoS on decoder output stream");
                decodeFrameResult = RESULT_EOS_REACHED;
            }
        } else {
            switch (tag) {
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    // Log.d(TAG, "Will try getting decoder output later");
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    sourceAudioFormat = decoder.getOutputFormat();
                    renderer.onMediaFormatChanged(sourceAudioFormat, targetFormat);
                    Log.d(TAG, "Decoder output format changed: " + sourceAudioFormat);
                    break;
                default:
                    Log.e(TAG, "Unhandled value " + tag + " when receiving decoded input frame");
                    break;
            }
        }

        return decodeFrameResult;
    }

    private int writeEncodedOutputFrame() throws TrackTranscoderException {
        int encodeFrameResult = RESULT_FRAME_PROCESSED;

        int tag = encoder.dequeueOutputFrame(0);
        if (tag >= 0) {
            Frame frame = encoder.getOutputFrame(tag);
            if (frame == null) {
                throw new TrackTranscoderException(TrackTranscoderException.Error.NO_FRAME_AVAILABLE);
            }

            if ((frame.bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "Encoder produced EoS, we are done");
                progress = 1.0f;
                encodeFrameResult = RESULT_EOS_REACHED;
            } else if (frame.bufferInfo.size > 0
                    && (frame.bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                mediaMuxer.writeSampleData(targetTrack, frame.buffer, frame.bufferInfo);
                if (duration > 0) {
                    progress = ((float) frame.bufferInfo.presentationTimeUs) / duration;
                }
            }

            encoder.releaseOutputFrame(tag);
        } else {
            switch (tag) {
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    //                        Log.d(TAG, "Will try getting encoder output buffer later");
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    // TODO for now, we assume that we only get one media format as a first buffer
                    MediaFormat outputMediaFormat = encoder.getOutputFormat();
                    if (!targetTrackAdded) {
                        targetFormat = outputMediaFormat;
                        targetTrack = mediaMuxer.addTrack(outputMediaFormat, targetTrack);
                        targetTrackAdded = true;
                        renderer.onMediaFormatChanged(sourceAudioFormat, targetFormat);
                    }
                    encodeFrameResult = RESULT_OUTPUT_MEDIA_FORMAT_CHANGED;
                    Log.d(TAG, "Encoder output format received " + outputMediaFormat);
                    break;
                default:
                    Log.e(TAG, "Unhandled value " + tag + " when receiving encoded output frame");
                    break;
            }
        }

        return encodeFrameResult;
    }
}
