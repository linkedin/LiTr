/*
 * Copyright 2023 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */

#ifndef LITR_MEDIAMUXER_H
#define LITR_MEDIAMUXER_H

#define STATUS_OK 0
#define STATUS_ERROR -1

#include "FFmpeg.h"

class MediaMuxer
{
public:
    MediaMuxer();
    ~MediaMuxer();

    int init(const char *path, const char *formatName);
    int start(const char **keys, const char **values, int optionSize);
    int stop();
    int addVideoStream(const char * codec_name, int64_t bitrate, int width, int height,
                       uint8_t *extradata, int extradata_size);
    int addAudioStream(const char * codec_name, int64_t bitrate, int channels, int sample_rate,
                       int frame_size, uint8_t *extradata, int extradata_size);
    int writeSampleData(int stream_index, uint8_t *buffer, int size, int64_t ptsUs, int flags);

private:
    AVFormatContext *mContext = nullptr;

    AVStream* addStream(const char * codec_name, int64_t bitrate, uint8_t *extradata, int extradata_size);
    static void logErrorCode(const char * error, int errorCode);
};

#endif //LITR_MEDIAMUXER_H
