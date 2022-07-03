package com.linkedin.android.litr.filter.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.net.Uri
import com.linkedin.android.litr.codec.Decoder
import com.linkedin.android.litr.codec.Frame
import com.linkedin.android.litr.codec.MediaCodecDecoder
import com.linkedin.android.litr.exception.TrackTranscoderException
import com.linkedin.android.litr.filter.BufferFilter
import com.linkedin.android.litr.io.MediaExtractorMediaSource
import com.linkedin.android.litr.io.MediaSource
import com.linkedin.android.litr.render.AudioProcessor
import com.linkedin.android.litr.render.AudioProcessorFactory
import com.linkedin.android.litr.transcoder.TrackTranscoder
import com.linkedin.android.litr.utils.ByteBufferPool
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingDeque
import kotlin.math.ceil
import kotlin.math.min

private const val BYTES_PER_SAMPLE = 2 // android uses 2 bytes per audio sample
private const val UNDEFINED_VALUE = -1

private const val TAG = "AudioOverlayFilter"

class AudioOverlayFilter(
    private val mediaSource: MediaSource,
    private val decoder: Decoder,
    private var overlayTrack: Int
) : BufferFilter {

    @JvmOverloads
    constructor(
        context: Context,
        uri: Uri,
        overlayTrack: Int = -1
    ) : this(
        MediaExtractorMediaSource(context, uri),
        MediaCodecDecoder(),
        overlayTrack
    )

    private val overlayChannelCount: Int
    private val overlaySampleRate: Int

    private val renderQueue = LinkedBlockingDeque<ByteBuffer>()
    private val bufferPool = ByteBufferPool(true)
    private val audioProcessorFactory = AudioProcessorFactory()
    private var audioProcessor: AudioProcessor? = null

    private var channelCount = UNDEFINED_VALUE
    private var sampleRate = UNDEFINED_VALUE
    private var samplingRatio = 1.0
    private var allOverlayFramesRead: Boolean = false

    init {
        val mediaFormats = mutableListOf<MediaFormat>()
        for (track in 0 until mediaSource.trackCount) {
            mediaFormats.add(track, mediaSource.getTrackFormat(track))
        }

        val firstAudioTrack = mediaFormats.indexOfFirst { mediaFormat ->
            mediaFormat.containsKey(MediaFormat.KEY_MIME) &&
                mediaFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio") == true
        }

        overlayTrack = if (firstAudioTrack >= 0) {
            firstAudioTrack
        } else {
            throw IllegalArgumentException("Audio overlay does not have an audio track")
        }

        val audioMediaFormat = mediaFormats[overlayTrack]

        overlayChannelCount = if (audioMediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            audioMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else {
            throw IllegalArgumentException("Audio overlay track must have channel count in MediaFormat")
        }

        overlaySampleRate = if (audioMediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            audioMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else {
            throw IllegalArgumentException("Audio overlay track must have channel count in MediaFormat")
        }
    }

    override fun init(mediaFormat: MediaFormat?) {
        channelCount = if (mediaFormat?.containsKey(MediaFormat.KEY_CHANNEL_COUNT) == true) {
            mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else {
            UNDEFINED_VALUE
        }

        sampleRate = if (mediaFormat?.containsKey(MediaFormat.KEY_SAMPLE_RATE) == true) {
            mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else {
            UNDEFINED_VALUE
        }

        samplingRatio = sampleRate.toDouble() / overlaySampleRate.toDouble()

        audioProcessor?.release()
        audioProcessor = audioProcessorFactory.createAudioProcessor(
            mediaSource.getTrackFormat(overlayTrack),
            mediaFormat
        )

        mediaSource.selectTrack(overlayTrack)

        decoder.init(mediaSource.getTrackFormat(overlayTrack), null)
        decoder.start()
    }

    override fun apply(frame: Frame) {
        frame.buffer?.let { frameBuffer ->
            while (!sufficientOverlayFramesInQueue(frameBuffer) && !allOverlayFramesRead) {
                // if we don't have enough overlay frames to apply to incoming audio frame, read more
                getNextOverlayFrame()?.let { renderQueue.add(it.buffer) } ?: break
            }

            renderOverlay(frameBuffer)
        }
    }

    override fun release() {
        decoder.stop()
        decoder.release()
        mediaSource.release()
        bufferPool.clear()
    }

    private fun sufficientOverlayFramesInQueue(frameBuffer: ByteBuffer): Boolean {
        val bytesInRenderQueue = renderQueue.sumBy { overlayBuffer ->
            overlayBuffer?.let { it.limit() - it.position() } ?: 0
        }

        return frameBuffer.remaining() <= bytesInRenderQueue
    }

    private fun getNextOverlayFrame(): Frame? {
        while (mediaSource.sampleTrackIndex != overlayTrack &&
            mediaSource.sampleTrackIndex != TrackTranscoder.NO_SELECTED_TRACK) {
            // if source contains multiple tracks, skip samples for other tracks
            // until we are reading from the track we need
            mediaSource.advance()
        }

        // get the input frame from the decoder, we will read into it
        var inputTag = MediaCodec.INFO_TRY_AGAIN_LATER
        while (inputTag < 0) {
            inputTag = decoder.dequeueInputFrame(-1)
        }
        val decoderInputFrame = decoder.getInputFrame(inputTag)
            ?: throw TrackTranscoderException(TrackTranscoderException.Error.NO_FRAME_AVAILABLE)

        val bytesRead = mediaSource.readSampleData(decoderInputFrame.buffer!!, 0)
        val sampleTime = mediaSource.sampleTime
        val sampleFlags = mediaSource.sampleFlags

        if (bytesRead <= 0 || (sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            // nothing was read from the source
            allOverlayFramesRead = true
            return null
        }

        // send the frame to decoder and advance the source position
        decoderInputFrame.bufferInfo[0, bytesRead, sampleTime] = sampleFlags
        decoder.queueInputFrame(decoderInputFrame)
        mediaSource.advance()

        // get the decoder output (decoded frame)
        var outputTag = MediaCodec.INFO_TRY_AGAIN_LATER
        while (outputTag < 0) {
            outputTag = decoder.dequeueOutputFrame(-1)
        }

        val decoderOutputFrame = decoder.getOutputFrame(outputTag)
            ?: throw TrackTranscoderException(TrackTranscoderException.Error.NO_FRAME_AVAILABLE)

        // resize decoded overlay frame to match the target frame it will be applied to
        val sourceSampleCount = decoderOutputFrame.bufferInfo.size / (BYTES_PER_SAMPLE * overlayChannelCount)
        val estimatedTargetSampleCount = ceil(sourceSampleCount * samplingRatio).toInt()
        val targetBufferCapacity = estimatedTargetSampleCount * channelCount * BYTES_PER_SAMPLE

        val processedBuffer = bufferPool.get(targetBufferCapacity)
        val processedFrame = Frame(outputTag, processedBuffer, MediaCodec.BufferInfo())
        audioProcessor?.processFrame(decoderOutputFrame, processedFrame)

        decoder.releaseOutputFrame(outputTag, false)

        return processedFrame
    }

    private fun renderOverlay(frameBuffer: ByteBuffer) {
        if (renderQueue.isEmpty()) {
            return
        }

        while (renderQueue.isNotEmpty() && frameBuffer.remaining() > 0) {
            renderQueue.peek()?.let { overlayBuffer ->
                applyOverlaySample(
                    frameBuffer,
                    overlayBuffer,
                    min(frameBuffer.remaining(), overlayBuffer.remaining())
                )

                if (overlayBuffer.remaining() == 0) {
                    renderQueue.removeFirst()
                    bufferPool.put(overlayBuffer)
                }
            }
        }

        // reset the position back to 0 by flipping the frame to "read" mode
        frameBuffer.flip()
    }

    private fun applyOverlaySample(frameBuffer: ByteBuffer, overlayBuffer: ByteBuffer, byteCount: Int) {
        repeat(byteCount / 2) {
            val mixedValue = frameBuffer.short + overlayBuffer.short
            frameBuffer.putShort(frameBuffer.position() - 2, mixedValue.toShort())
        }
    }
}
