/*
 * Copyright 2023 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */

#include <string>

#include "MediaMuxer.h"
#include "FFmpeg.h"
#include "Logging.h"

// References:
//  - https://github.com/FFmpeg/FFmpeg/blob/n3.0/doc/examples/muxing.c#L569
//  - https://github.com/leandromoreira/ffmpeg-libav-tutorial

MediaMuxer::MediaMuxer() = default;

MediaMuxer::~MediaMuxer() {
    if (!mContext) {
        return;
    }

    if (mContext->pb != nullptr) {
        // Close output file.
        avio_closep(&mContext->pb);
    }

    // Free the context.
    avformat_free_context(mContext);
}

int MediaMuxer::init(const char *path, const char *formatName) {
    int err = avformat_alloc_output_context2(&mContext, nullptr, formatName, path);
    if (err < 0) {
        logErrorCode("Failed to allocate AVFormatContext", err);
        return STATUS_ERROR;
    }

    return STATUS_OK;
}

int MediaMuxer::start(const char *keys[], const char *values[], int optionSize) {
    int err;

    // Open File.
    err = avio_open(&mContext->pb, mContext->url, AVIO_FLAG_WRITE);
    if (err < 0) {
        logErrorCode("Failed to open file", err);
        return STATUS_ERROR;
    }

    // Build the muxer's options with the keys and corresponding values that were provided.
    AVDictionary *opts = nullptr;
    for (int i = 0; i < optionSize; i++) {
        av_dict_set(&opts, keys[i], values[i], 0);
    }

    // Write the stream header, if any.
    err = avformat_write_header(mContext, &opts);
    if (err < 0) {
        logErrorCode("Failed to write stream header", err);
        return STATUS_ERROR;
    }

    return STATUS_OK;
}

int MediaMuxer::stop() {
    // Write the trailer, if any.
    int err = av_write_trailer(mContext);
    if (err < 0) {
        logErrorCode("Failed to write trailer", err);
        return STATUS_ERROR;
    }

    // Close output file.
    avio_closep(&mContext->pb);

    // Free the context.
    avformat_free_context(mContext);
    mContext = nullptr;
    return STATUS_OK;
}

int MediaMuxer::addVideoStream(const char *codec_name, int64_t bitrate, int width, int height,
                               uint8_t *extradata, int extradata_size) {
    AVStream* stream = addStream(codec_name, bitrate, extradata, extradata_size);
    if (!stream) {
        LOGE("Failed to create new stream");
        return STATUS_ERROR;
    }

    // Add the video specific stream details.
    stream->codecpar->width = width;
    stream->codecpar->height = height;

    return stream->index;
}

int MediaMuxer::addAudioStream(const char *codec_name, int64_t bitrate, int channels,
                               int sample_rate, int frame_size, uint8_t *extradata,
                               int extradata_size) {
    AVStream* stream = addStream(codec_name, bitrate, extradata, extradata_size);
    if (!stream) {
        LOGE("Failed to create new stream");
        return STATUS_ERROR;
    }

    // Add the audio specific stream details.
    stream->codecpar->channels = channels;
    stream->codecpar->sample_rate = sample_rate;
    stream->codecpar->frame_size = frame_size;

    return stream->index;
}

AVStream* MediaMuxer::addStream(const char *codec_name, int64_t bitrate, uint8_t *extradata, int extradata_size) {
    AVStream* stream = avformat_new_stream(mContext, nullptr);
    if (!stream) {
        LOGE("Failed to allocate new stream");
        return nullptr;
    }

    // Look up the AVCodecDescriptor based upon it's name. If we don't locate/understand it, we are
    // unable to determine the codec type (video, audio, etc) as well as it's AVCodecID.
    const AVCodecDescriptor* descriptor = avcodec_descriptor_get_by_name(codec_name);
    if (!descriptor) {
        LOGE("Failed to identify AVCodecDescriptor by name");
        return nullptr;
    }

    // Build known, common, stream details...
    stream->codecpar->codec_type = descriptor->type;
    stream->codecpar->codec_id = descriptor->id;
    stream->codecpar->bit_rate = bitrate;

    // If extra data was provided, we should copy it for the stream.
    if (extradata && extradata_size) {
        stream->codecpar->extradata_size = extradata_size;
        stream->codecpar->extradata = static_cast<uint8_t *>(av_mallocz(
                extradata_size + AV_INPUT_BUFFER_PADDING_SIZE));
        memcpy(stream->codecpar->extradata, extradata, extradata_size);
    }

    // We will set all streams to have a time base that is in microseconds. This is because we
    // expect all PTS values provided to be in those units, since that's what Android provides.
    stream->time_base = (AVRational){ 1, 1000000 };

    return stream;
}

int MediaMuxer::writeSampleData(int stream_index, uint8_t *buffer, int size, int64_t ptsUs, int flags) {
    // Build the packet that we will attempt to write.
    AVPacket pkt = { 0 }; // Data and size must be 0;
    av_init_packet(&pkt);

    // Populate the packet data with what we've been given. Since we're using the buffer directly
    // we will not wrap it in a AVBuffer/Ref instance.
    pkt.stream_index = stream_index;
    pkt.data = buffer;
    pkt.size = size;
    pkt.dts = ptsUs;
    pkt.pts = ptsUs;
    pkt.flags = flags;

    // While we originally set the ideal time base of the stream to be in microseconds, the muxer
    // is allowed to change this. We will therefore need to scale our given PTS (in microseconds) to
    // something suitable for the specific stream.
    auto stream = mContext->streams[stream_index];
    av_packet_rescale_ts(&pkt, (AVRational){ 1, 1000000 }, stream->time_base);

    LOGI("writeSampleData(index: %d size: %d pts: %lld flags: %d)", stream_index, size, ptsUs, flags);

    // Write the compressed frame to the media file.
    int err = av_interleaved_write_frame(mContext, &pkt);
    if (err < 0) {
        logErrorCode("Failed to write frame", err);
        return STATUS_ERROR;
    }

    return STATUS_OK;
}

void MediaMuxer::logErrorCode(const char *error, int errorCode) {
    char errorStr[AV_ERROR_MAX_STRING_SIZE] = {0};
    av_make_error_string(errorStr, AV_ERROR_MAX_STRING_SIZE, errorCode);

    if (errorStr) {
        LOGE("%s: %s", error, errorStr);
    } else {
        LOGE("%s: %d", error, errorCode);
    }
}
