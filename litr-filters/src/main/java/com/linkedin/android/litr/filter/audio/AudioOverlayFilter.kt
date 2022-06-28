package com.linkedin.android.litr.filter.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.linkedin.android.litr.codec.Frame
import com.linkedin.android.litr.codec.MediaCodecDecoder
import com.linkedin.android.litr.exception.TrackTranscoderException
import com.linkedin.android.litr.filter.BufferFilter
import com.linkedin.android.litr.io.MediaExtractorMediaSource
import com.linkedin.android.litr.render.AudioProcessor
import com.linkedin.android.litr.render.AudioProcessorFactory
import com.linkedin.android.litr.transcoder.TrackTranscoder
import com.linkedin.android.litr.utils.ByteBufferPool
import kotlin.math.ceil

private const val BYTES_PER_SAMPLE = 2 // android uses 2 bytes per audio sample
private const val UNDEFINED_VALUE = -1

private const val TAG = "AudioOverlayFilter"

class AudioOverlayFilter(
    context: Context,
    uri: Uri,
    private val overlayTrack: Int = 0
) : BufferFilter {

    private val mediaSource = MediaExtractorMediaSource(context, uri)
    private val decoder = MediaCodecDecoder()

    private val overlayChannelCount: Int
    private val overlaySampleRate: Int

    private val bufferPool = ByteBufferPool(true)
    private lateinit var audioProcessor: AudioProcessor

    private var channelCount = UNDEFINED_VALUE
    private var sampleRate = UNDEFINED_VALUE
    private var samplingRatio = 1.0

    init {
        val mediaFormats = mutableListOf<MediaFormat>()
        for (track in 0 until mediaSource.trackCount) {
            mediaFormats.add(track, mediaSource.getTrackFormat(track))
        }
        val audioMediaFormat = mediaFormats.firstOrNull { mediaFormat ->
            mediaFormat.containsKey(MediaFormat.KEY_MIME) &&
                mediaFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio") == true
        } ?: throw IllegalArgumentException("Audio overlay does not have an audio track")

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

        audioProcessor = AudioProcessorFactory().createAudioProcessor(
            mediaSource.getTrackFormat(overlayTrack),
            mediaFormat
        )

        mediaSource.selectTrack(overlayTrack)

        decoder.init(mediaSource.getTrackFormat(overlayTrack), null)
        decoder.start()
    }

    override fun apply(frame: Frame) {
        Log.d(TAG, "frame ${frame.bufferInfo.presentationTimeUs}")
        val overlayBuffer = getNextOverlayFrame()
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

        Log.d(TAG, "Overlay frame sample time $sampleTime")

        if (bytesRead == 0) {
            // nothing was read from the source
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
        audioProcessor.processFrame(decoderOutputFrame, processedFrame)

        decoder.releaseOutputFrame(outputTag, false)

        return processedFrame
    }
}