package com.linkedin.android.litr.thumbnails

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.linkedin.android.litr.exception.TrackTranscoderException
import com.linkedin.android.litr.transcoder.TrackTranscoder
import com.linkedin.android.litr.utils.TranscoderUtils
import java.util.concurrent.TimeUnit


class ThumbnailExtractJob constructor(
    private val jobId: String,
    private val params: ThumbnailExtractParameters,
    private val listener: ThumbnailExtractListener?
) : Runnable {

    private lateinit var sourceVideoFormat: MediaFormat
    private var sourceTrack: Int = -1

    private val decoder = params.decoder
    private val mediaSource = params.mediaSource
    private val renderer = params.renderer

    private var lastExtractFrameResult: Int = 0
    private var lastDecodeFrameResult: Int = 0

    override fun run() {
        try {
            extract()
        } catch (e: RuntimeException) {
            Log.e(TAG, "ThumbnailExtractJob error", e)
            when (e.cause) {
                is InterruptedException -> stop()
                else -> error(e)
            }
        }
    }

    private fun extract() {
        // Initialize
        sourceTrack = TranscoderUtils.getFirstVideoTrackIndex(mediaSource)
        if (sourceTrack < 0) {
            return
        }

        sourceVideoFormat = mediaSource.getTrackFormat(sourceTrack)
        renderer.init(params.sourceSize.x, params.sourceSize.y, mediaSource.orientationHint)
        decoder.init(sourceVideoFormat, renderer.inputSurface)

        // Start
        mediaSource.selectTrack(sourceTrack)
        decoder.start()
        listener?.onStarted(jobId)

        // Run to completion
        var completed: Boolean

        do {
            completed = processNextFrame()
            if (Thread.interrupted()) {
                completed = true
            }
        } while (!completed)

        release(completed)
    }

    @VisibleForTesting
    fun processNextFrame(): Boolean {

        // extract the frame from the incoming stream and send it to the decoder
        if (lastExtractFrameResult != TrackTranscoder.RESULT_EOS_REACHED) {
            lastExtractFrameResult = extractAndEnqueueInputFrame()
        }

        if (lastDecodeFrameResult != TrackTranscoder.RESULT_EOS_REACHED) {
            lastDecodeFrameResult = renderDecodedFrame()
        }

        return (lastExtractFrameResult == TrackTranscoder.RESULT_EOS_REACHED &&
                lastDecodeFrameResult == TrackTranscoder.RESULT_EOS_REACHED)
    }


    private fun extractAndEnqueueInputFrame(): Int {
        var extractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED

        val tag = decoder.dequeueInputFrame(0)
        if (tag >= 0) {
            val frame = decoder.getInputFrame(tag)!!
            val buffer = frame.buffer!!

            val bytesRead = mediaSource.readSampleData(buffer, 0)
            val sampleTime = mediaSource.sampleTime
            val sampleFlags = mediaSource.sampleFlags

            Log.d(TAG, "Read $bytesRead from source")

            if (bytesRead < 0 || (sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                frame.bufferInfo[0, 0, -1] = MediaCodec.BUFFER_FLAG_END_OF_STREAM
                decoder.queueInputFrame(frame)
                extractFrameResult = TrackTranscoder.RESULT_EOS_REACHED
                Log.d(TAG, "EoS reached on the input stream")
            } else if (sampleTime >= params.frameProvider.mediaRange.end) {
                frame.bufferInfo[0, 0, -1] = MediaCodec.BUFFER_FLAG_END_OF_STREAM
                decoder.queueInputFrame(frame)
                extractFrameResult = TrackTranscoder.RESULT_EOS_REACHED
                Log.d(TAG, "EoS reached on the input stream")
            } else {
                frame.bufferInfo[0, bytesRead, sampleTime] = sampleFlags
                decoder.queueInputFrame(frame)
                mediaSource.advance()
            }
        }
        return extractFrameResult
    }

    /**
     * Reads output frame from the decoder, and sends it to the renderer for thumbnail extraction.
     */
    private fun renderDecodedFrame(): Int {
        var decodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED
        val tag = decoder.dequeueOutputFrame(0)
        if (tag >= 0) {
            val frame = decoder.getOutputFrame(tag) ?: throw TrackTranscoderException(TrackTranscoderException.Error.NO_FRAME_AVAILABLE)
            if ((frame.bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "EoS on decoder output stream")
                decoder.releaseOutputFrame(tag, false)
                // TODO: Handle EOS?
                decodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED
            } else {
                val frameProvider = params.frameProvider
                val presentationTimeUs = frame.bufferInfo.presentationTimeUs

                val isFrameAfterSelectionStart = presentationTimeUs >= frameProvider.mediaRange.start
                val shouldExtract = frameProvider.shouldExtract(presentationTimeUs)

                // Determines if decoder should update its surface, and if the renderer should create a thumbnail for this frame
                val shouldRender = isFrameAfterSelectionStart && shouldExtract

                decoder.releaseOutputFrame(tag, shouldRender)

                if (shouldRender) {
                    renderer.renderFrame(
                        TimeUnit.MICROSECONDS.toNanos(presentationTimeUs - frameProvider.mediaRange.start)
                    )
                    listener?.onExtracted(jobId, presentationTimeUs)
                    frameProvider.didExtract(presentationTimeUs)
                }
            }
        } else {
            when (tag) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                }
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    sourceVideoFormat = decoder.outputFormat
                    Log.d(TAG, "Decoder output format changed: $sourceVideoFormat")
                }
                else -> Log.e(TAG, "Unhandled value $tag when receiving decoded input frame")
            }
        }
        return decodeFrameResult
    }

    fun stop() {
        release(true)
    }

    fun error(cause: Throwable?) {
        release(false)
        listener?.onError(jobId, cause)
    }

    fun release(success: Boolean) {
        decoder.stop()
        decoder.release()

        mediaSource.release()

        if (success) {
            listener?.onCompleted(jobId)
        }
    }

    companion object {
        private val TAG: String = ThumbnailExtractJob::class.java.simpleName
    }
}