package com.linkedin.android.litr.recorder

import android.graphics.Point
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.linkedin.android.litr.codec.Encoder
import com.linkedin.android.litr.codec.Frame
import com.linkedin.android.litr.exception.TrackTranscoderException
import com.linkedin.android.litr.io.MediaTarget
import com.linkedin.android.litr.recorder.readers.SurfaceTrackReader
import com.linkedin.android.litr.render.GlVideoRenderer
import com.linkedin.android.litr.transcoder.TrackTranscoder.*
import com.linkedin.android.litr.utils.TranscoderUtils


/**
 * Media encoder that processes video frames continuously until stopped.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class SurfaceMediaRecorder internal constructor(
    val reader: SurfaceTrackReader,
    val sourceFormat: MediaFormat,
    val mediaTarget: MediaTarget,
    private var targetTrack: Int,
    private var targetFormat: MediaFormat,
    @VisibleForTesting
    val renderer: GlVideoRenderer,
    val encoder: Encoder
) : MediaRecorder {
    private var targetTrackAdded: Boolean = false

    @VisibleForTesting
    var lastEncodeFrameResult: Int = RESULT_FRAME_PROCESSED

    init {
        initCodecs()
    }

    @Throws(TrackTranscoderException::class)
    private fun initCodecs() {
        if (sourceFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
            val sourceFrameRate = sourceFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
            targetFormat.setInteger(MediaFormat.KEY_FRAME_RATE, sourceFrameRate)
        }

        encoder.init(targetFormat)
        renderer.init(encoder.createInputSurface(), sourceFormat, targetFormat)

        val size = TranscoderUtils.getDimensions(targetFormat) ?: Point(1, 1)
        renderer.prepareInputSurface(size.x, size.y)
    }

    @Throws(TrackTranscoderException::class)
    override fun start() {
        reader.start()
        encoder.start()
    }

    override fun stop() {
        reader.stop()
        encoder.stop()
        encoder.release()
        renderer.release()
    }

    @Throws(TrackTranscoderException::class)
    override fun processNextFrame(): Int {
        if (!encoder.isRunning) {
            // can't do any work
            return ERROR_TRANSCODER_NOT_RUNNING
        }

        val ptime = System.nanoTime()
        renderer.inputSurface?.let {
            reader.drawFrame(it, ptime)
        }
        renderer.renderFrame(null, ptime)

        // get the encoded frame and write it into the target file
        if (lastEncodeFrameResult != RESULT_EOS_REACHED) {
            lastEncodeFrameResult = writeEncodedOutputFrame()
        }

        return lastEncodeFrameResult
    }

    @Throws(TrackTranscoderException::class)
    private fun writeEncodedOutputFrame(): Int {
        var encodeFrameResult: Int = RESULT_FRAME_PROCESSED
        val index: Int = encoder.dequeueOutputFrame(0)
        if (index >= 0) {
            val frame: Frame = encoder.getOutputFrame(index)
                ?: throw TrackTranscoderException(TrackTranscoderException.Error.NO_FRAME_AVAILABLE)

            if (frame.bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                Log.d(TAG, "Encoder produced EoS, we are done")
                encodeFrameResult = RESULT_EOS_REACHED
            } else if (frame.bufferInfo.size > 0 && frame.bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                mediaTarget.writeSampleData(targetTrack, frame.buffer!!, frame.bufferInfo)
            }
            encoder.releaseOutputFrame(index)
        } else {
            when (index) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                }
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // TODO for now, we assume that we only get one media format as a first buffer
                    val outputMediaFormat: MediaFormat = encoder.outputFormat
                    if (!targetTrackAdded) {
                        targetFormat = outputMediaFormat
                        targetTrack = mediaTarget.addTrack(outputMediaFormat, targetTrack)
                        targetTrackAdded = true
                        renderer.onMediaFormatChanged(sourceFormat, targetFormat)
                    }
                    encodeFrameResult = RESULT_OUTPUT_MEDIA_FORMAT_CHANGED
                    Log.d(TAG, "Encoder output format received $outputMediaFormat")
                }
                else -> Log.e(TAG, "Unhandled value $index when receiving encoded output frame")
            }
        }
        return encodeFrameResult
    }

    companion object {
        private val TAG: String = SurfaceMediaRecorder::class.java.simpleName
    }
}