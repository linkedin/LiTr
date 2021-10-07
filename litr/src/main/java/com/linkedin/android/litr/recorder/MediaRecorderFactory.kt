package com.linkedin.android.litr.recorder

import android.media.MediaFormat
import com.linkedin.android.litr.exception.TrackTranscoderException
import com.linkedin.android.litr.recorder.readers.SurfaceTrackReader
import com.linkedin.android.litr.render.GlVideoRenderer


class MediaRecorderFactory {

    @Throws(TrackTranscoderException::class)
    fun create(params: MediaRecordParameters): MediaRecorder {
        val trackMimeType = params.targetFormat.getString(MediaFormat.KEY_MIME) ?: throw TrackTranscoderException(
            TrackTranscoderException.Error.SOURCE_TRACK_MIME_TYPE_NOT_FOUND,
            params.targetFormat,
            null,
            null
        )

        val isVideo = trackMimeType.startsWith("video")
        val isAudio = trackMimeType.startsWith("audio")

        return when {
            isVideo -> {
                SurfaceMediaRecorder(
                    params.reader as? SurfaceTrackReader ?: throw TrackTranscoderException(
                        TrackTranscoderException.Error.READER_NOT_COMPATIBLE,
                        params.targetFormat,
                        null,
                        null
                    ),
                    params.sourceFormat,
                    params.mediaTarget,
                    params.targetTrack,
                    params.targetFormat,
                    (params.renderer as? GlVideoRenderer) ?: throw TrackTranscoderException(
                        TrackTranscoderException.Error.RENDERER_NOT_COMPATIBLE,
                        params.targetFormat,
                        null,
                        null
                    ),
                    params.encoder
                )
            }
            // TODO: Handle audio/other/passthrough reader types, have descriptive exception
            else -> throw TrackTranscoderException(TrackTranscoderException.Error.SOURCE_TRACK_MIME_TYPE_NOT_FOUND, params.targetFormat, null, null)
        }

    }
}