package com.linkedin.android.litr.thumbnails

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC
import android.media.MediaFormat
import android.text.TextUtils
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.linkedin.android.litr.exception.TrackTranscoderException
import com.linkedin.android.litr.transcoder.TrackTranscoder
import com.linkedin.android.litr.utils.TranscoderUtils
import java.io.File
import java.util.concurrent.TimeUnit


class ThumbnailExtractJob @JvmOverloads constructor(
    val jobId: String,
    val params: ThumbnailExtractParameters,
    private val listener: ThumbnailExtractListener?
) : Runnable {

    private var bitrate: Int = 0
    private lateinit var sourceVideoFormat: MediaFormat

    val decoder = params.decoder
    val mediaSource = params.mediaSource
    val sourceTrack = params.sourceTrack
    val renderer = params.renderer

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
        init()
        start()

        listener?.onStarted(jobId)

        var completed: Boolean

        do {
            completed = processNextFrame()

            if (Thread.interrupted()) {
                completed = true
            }
        } while (!completed)

        release(completed)

    }

    private fun start() {
        mediaSource.selectTrack(sourceTrack)
        decoder.start()
    }

    private fun init() {

        sourceVideoFormat = mediaSource.getTrackFormat(sourceTrack)

        bitrate = TranscoderUtils.estimateVideoTrackBitrate(mediaSource, sourceTrack)

        renderer.init(null, sourceVideoFormat, sourceVideoFormat)
        decoder.init(sourceVideoFormat, renderer.inputSurface)
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
            } else if (sampleTime >= params.frameProvider.range.end) {
                frame.bufferInfo[0, 0, -1] = MediaCodec.BUFFER_FLAG_END_OF_STREAM
                decoder.queueInputFrame(frame)
                advanceToNextTrack()
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

    private fun advanceToNextTrack() {
        // done with this track, advance until track switches to let other track transcoders finish work
        while (mediaSource.sampleTrackIndex == sourceTrack) {
            mediaSource.advance()
            if (mediaSource.sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                // reached the end of container, no more tracks left
                return
            }
        }
    }

    private fun resizeDecodedInputFrame(): Int {
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
                val isFrameAfterSelectionStart = presentationTimeUs >= frameProvider.range.start
                val shouldExtract = frameProvider.shouldExtract(presentationTimeUs)
                val shouldRender = isFrameAfterSelectionStart && shouldExtract
                decoder.releaseOutputFrame(tag, shouldRender)
                if (shouldRender) {
                    renderer.renderFrame(
                        null,
                        TimeUnit.MICROSECONDS.toNanos(presentationTimeUs - frameProvider.range.start)
                    )
                    frameProvider.didExtract(presentationTimeUs)
                    Log.d(TAG, "Rendered frame")
                }
            }
        } else {
            when (tag) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                }
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    sourceVideoFormat = decoder.outputFormat
                    renderer.onMediaFormatChanged(sourceVideoFormat, sourceVideoFormat)
                    Log.d(TAG, "Decoder output format changed: $sourceVideoFormat")
                }
                else -> Log.e(TAG, "Unhandled value $tag when receiving decoded input frame")
            }
        }
        return decodeFrameResult
    }

    @VisibleForTesting
    fun processNextFrame(): Boolean {

        // extract the frame from the incoming stream and send it to the decoder
        if (lastExtractFrameResult != TrackTranscoder.RESULT_EOS_REACHED) {
            lastExtractFrameResult = extractAndEnqueueInputFrame()
        }

        if (lastDecodeFrameResult != TrackTranscoder.RESULT_EOS_REACHED) {
            lastDecodeFrameResult = resizeDecodedInputFrame()
        }

        return (lastExtractFrameResult == TrackTranscoder.RESULT_EOS_REACHED &&
                lastDecodeFrameResult == TrackTranscoder.RESULT_EOS_REACHED)
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

    @VisibleForTesting
    fun deleteOutputFile(outputFilePath: String?): Boolean {
        if (TextUtils.isEmpty(outputFilePath)) {
            return false
        }
        return outputFilePath?.let { File(it) }?.delete() ?: false
    }

    companion object {
        private val TAG: String = ThumbnailExtractJob::class.java.simpleName
    }
}