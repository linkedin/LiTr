package com.linkedin.android.litr.thumbnails

import android.graphics.Bitmap
import android.media.MediaCodec
import android.util.Log
import com.linkedin.android.litr.exception.TrackTranscoderException
import com.linkedin.android.litr.utils.TranscoderUtils
import java.util.concurrent.TimeUnit
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import com.linkedin.android.litr.codec.Decoder
import com.linkedin.android.litr.io.MediaSource
import java.lang.Exception
import kotlin.math.abs


class ThumbnailExtractJob constructor(
    private val jobId: String,
    private val params: ThumbnailExtractParameters,
    private val listener: ThumbnailExtractListener?
) : Runnable {
    private var sourceTrack: Int = -1

    private val syncFrameDecoder = params.syncFrameDecoder
    private val exactFrameDecoder = params.exactFrameDecoder
    private val renderer = params.renderer
    private val syncExtractState = ExtractState()
    private val exactExtractState = ExtractState()


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

    private fun getSyncFrameTimestamps(source: MediaSource): List<Long> {
        val result = mutableListOf<Long>()

        var lastSampleTime = -1L
        var sampleTime = source.sampleTime

        while (sampleTime >= 0L && sampleTime != lastSampleTime) {
            if (source.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                result.add(sampleTime)
            }
            source.seekTo(sampleTime + 1L, MediaExtractor.SEEK_TO_NEXT_SYNC)
            lastSampleTime = sampleTime
            sampleTime = source.sampleTime
        }

        return result
    }

    private fun mapSyncFramesToNearestRequestedFrames(syncFrames: List<Long>, requestedFrames: List<Long>): Map<Long, List<Long>> {
        val result = mutableMapOf<Long, MutableList<Long>>()
        // TODO: Optimization. This has M*N complexity, can be improved (e.g. use TreeSet to find higher/lower sync time, then choose nearest, to get MlogN)
        requestedFrames.forEach { frame ->
            val syncFrame = syncFrames.minByOrNull { syncFrame -> abs(frame - syncFrame) }
            if (syncFrame != null) {
                result.getOrPut(syncFrame, { mutableListOf() }).add(frame)
            }
        }
        return result
    }


    private fun extract() {
        if (params.timestampsUs.isEmpty()) {
            return
        }


        // MMR TEST
        listener?.onStarted(jobId, params.timestampsUs)

        val retriever = params.mediaSourceFactory.getRetriever()
        params.timestampsUs.forEachIndexed { index, frameTimeUs ->
            val fullBitmap = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val scaledBitmap = ThumbnailUtils.extractThumbnail(fullBitmap, params.destSize.x, params.destSize.y)
            listener?.onExtracted(jobId, index, scaledBitmap)
        }

        params.timestampsUs.forEachIndexed { index, frameTimeUs ->
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//                retriever.getScaledFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST, params.destSize.x, params.destSize.y)
//            } else {
//
//            }
            val fullBitmap = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            val scaledBitmap = ThumbnailUtils.extractThumbnail(fullBitmap, params.destSize.x, params.destSize.y)
            listener?.onExtracted(jobId, index, scaledBitmap)
        }

        return

        // Initialize
        val mediaSourceForSyncFrames = params.mediaSourceFactory.createMediaSource()
        sourceTrack = TranscoderUtils.getFirstVideoTrackIndex(mediaSourceForSyncFrames)
        if (sourceTrack < 0) {
            return // TODO: Handle error here
        }
        mediaSourceForSyncFrames.selectTrack(sourceTrack)

        // Start

        // Obtain sync frame timestamps
        // Media source used only for obtaining sync frames
        val syncFrameTimestamps = getSyncFrameTimestamps(mediaSourceForSyncFrames)

        if (syncFrameTimestamps.isEmpty()) {
            return // TODO: Handle error here
        }

        // Map sync frames to requested frames
        val syncFramesToNearestFrames = mapSyncFramesToNearestRequestedFrames(syncFrameTimestamps, params.timestampsUs)

        val extractSyncMediaSource = params.mediaSourceFactory.createMediaSource().apply {
            selectTrack(sourceTrack)
        }

        val extractExactMediaSource = params.mediaSourceFactory.createMediaSource().apply {
            selectTrack(sourceTrack)
        }

        val frameTimeToIndex = params.timestampsUs.mapIndexed { index, time ->
            time to index
        }.toMap()

        val extractSyncFramesNotifier = object : FrameExtractNotifier {
            override fun shouldExtract(presentationTimeUs: Long): Boolean {
                return syncFramesToNearestFrames.containsKey(presentationTimeUs)
            }

            override fun didExtract(presentationTimeUs: Long, bitmap: Bitmap?) {
                syncFramesToNearestFrames[presentationTimeUs]?.forEach { timestamp ->
                    frameTimeToIndex[timestamp]?.let {
                        listener?.onExtracted(jobId, it, bitmap)
                    }
                }
            }
        }

        var remainingFrames = params.timestampsUs.toMutableList()

        val extractExactFramesNotifier = object : FrameExtractNotifier {
            override fun shouldExtract(presentationTimeUs: Long): Boolean {
                return presentationTimeUs >= remainingFrames.first()
            }

            override fun didExtract(presentationTimeUs: Long, bitmap: Bitmap?) {
                val lastEarlierFrameIndex = remainingFrames.indexOfLast { it <= presentationTimeUs }
                if (lastEarlierFrameIndex >= 0) {
                    // List had elements smaller than the presentation time -- notify them and then remove
                    val toNotify = remainingFrames.subList(0, lastEarlierFrameIndex + 1)
                    toNotify.mapNotNull { frameTimeToIndex[it] }.forEach {
                        listener?.onExtracted(jobId, it, bitmap)
                    }
                    remainingFrames = remainingFrames.subList(lastEarlierFrameIndex + 1, remainingFrames.size)
                }
            }
        }

        renderer.init(params.sourceSize.x, params.sourceSize.y, params.destSize.y, params.destSize.y, mediaSourceForSyncFrames.orientationHint)
        syncFrameDecoder.init(mediaSourceForSyncFrames.getTrackFormat(sourceTrack), renderer.inputSurface)


        var completed = false

        try {

            syncFrameDecoder.start()
            listener?.onStarted(jobId, params.timestampsUs)

            do {
                // Extract all sync frames to EOF
                if (syncExtractState.lastExtractFrameResult == FrameResult.FrameProcessed) {
                    // Last frame was extracted, move to extract the next sync frame
                    extractSyncMediaSource.seekTo(extractSyncMediaSource.sampleTime + 1L, MediaExtractor.SEEK_TO_NEXT_SYNC)
                }
                completed = processFrame(extractSyncMediaSource, extractSyncFramesNotifier, syncExtractState, syncFrameDecoder)

                if (Thread.interrupted()) {
                    completed = false
                    break
                }
            } while (!completed)


            if (completed) {

                syncFrameDecoder.stop()
                syncFrameDecoder.release()

                exactFrameDecoder.init(mediaSourceForSyncFrames.getTrackFormat(sourceTrack), renderer.inputSurface)
                exactFrameDecoder.start()

                do {
                    completed = processFrame(extractExactMediaSource, extractExactFramesNotifier, exactExtractState, exactFrameDecoder)

                    if (Thread.interrupted()) {
                        completed = false
                        break
                    }
                } while (!completed)
            }

        } catch (ex: Exception) {
            Log.d("OH NO", ex.toString())
        } finally {
            mediaSourceForSyncFrames.release()
            extractSyncMediaSource.release()
            extractExactMediaSource.release()
        }

        release(completed)
    }

    private fun processFrame(mediaSource: MediaSource, notifier: FrameExtractNotifier, state: ExtractState, decoder: Decoder): Boolean {

        if (state.lastExtractFrameResult != FrameResult.EOSReached) {
            state.lastExtractFrameResult = extractAndEnqueueInputFrame(mediaSource, decoder)
        }

        if (state.lastDecodeFrameResult != FrameResult.EOSReached) {
            state.lastDecodeFrameResult = renderDecodedFrame(notifier, decoder)
        }

        return (state.lastExtractFrameResult == FrameResult.EOSReached &&
                state.lastDecodeFrameResult == FrameResult.EOSReached)
    }

    private fun extractAndEnqueueInputFrame(mediaSource: MediaSource, decoder: Decoder): FrameResult {
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
                Log.d(TAG, "EoS reached on the input stream")
                return FrameResult.EOSReached
            } else if (sampleTime >= params.mediaRange.end) {
                frame.bufferInfo[0, 0, -1] = MediaCodec.BUFFER_FLAG_END_OF_STREAM
                decoder.queueInputFrame(frame)
                Log.d(TAG, "EoS reached on the input stream")
                return FrameResult.EOSReached
            } else {
                frame.bufferInfo[0, bytesRead, sampleTime] = sampleFlags
                decoder.queueInputFrame(frame)
                mediaSource.advance()
                return FrameResult.FrameProcessed
            }
        }
        return FrameResult.FrameSkipped
    }

    /**
     * Reads output frame from the decoder, and sends it to the renderer for thumbnail extraction.
     */
    private fun renderDecodedFrame(notifier: FrameExtractNotifier, decoder: Decoder): FrameResult {
        val tag = decoder.dequeueOutputFrame(0)
        if (tag >= 0) {
            val frame = decoder.getOutputFrame(tag) ?: throw TrackTranscoderException(TrackTranscoderException.Error.NO_FRAME_AVAILABLE)
            if ((frame.bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "EoS on decoder output stream")
                decoder.releaseOutputFrame(tag, false)
                // TODO: Handle EOS?
                return FrameResult.EOSReached
            } else {
                val presentationTimeUs = frame.bufferInfo.presentationTimeUs

                val isFrameAfterSelectionStart = presentationTimeUs >= params.mediaRange.start
                val shouldRender = isFrameAfterSelectionStart && notifier.shouldExtract(presentationTimeUs)

                decoder.releaseOutputFrame(tag, shouldRender)

                if (shouldRender) {
                    val bitmap = renderer.renderFrame(
                        TimeUnit.MICROSECONDS.toNanos(presentationTimeUs - params.mediaRange.start)
                    )
                    notifier.didExtract(presentationTimeUs, bitmap)
                }

                return FrameResult.FrameProcessed
            }
        } else {
            when (tag) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    return FrameResult.FrameSkipped
                }
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    return FrameResult.MediaFormatChanged
                }
                else -> Log.e(TAG, "Unhandled value $tag when receiving decoded input frame")
            }
        }
        return FrameResult.FrameSkipped
    }

    fun stop() {
        release(true)
    }

    fun error(cause: Throwable?) {
        release(false)
        listener?.onError(jobId, cause)
    }

    fun release(success: Boolean) {
        syncFrameDecoder.stop()
        syncFrameDecoder.release()
        exactFrameDecoder.stop()
        exactFrameDecoder.release()

        if (success) {
            listener?.onCompleted(jobId)
        }
    }

    companion object {
        private val TAG: String = ThumbnailExtractJob::class.java.simpleName
    }
}

enum class FrameResult {
    Unknown,
    MediaFormatChanged,
    FrameProcessed,
    EOSReached,
    FrameSkipped
}

data class ExtractState(
    var lastExtractFrameResult: FrameResult = FrameResult.Unknown,
    var lastDecodeFrameResult: FrameResult = FrameResult.Unknown
)

interface FrameExtractNotifier {
    fun shouldExtract(presentationTimeUs: Long): Boolean
    fun didExtract(presentationTimeUs: Long, bitmap: Bitmap?)
}