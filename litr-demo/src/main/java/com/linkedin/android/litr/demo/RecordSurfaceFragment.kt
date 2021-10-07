package com.linkedin.android.litr.demo

import android.content.pm.ActivityInfo
import android.graphics.Paint
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Bundle
import android.text.TextPaint
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import com.linkedin.android.litr.codec.MediaCodecEncoder
import com.linkedin.android.litr.demo.data.TargetMedia
import com.linkedin.android.litr.demo.databinding.FragmentRecordSurfaceBinding
import com.linkedin.android.litr.io.MediaMuxerMediaTarget
import com.linkedin.android.litr.io.MediaTarget
import com.linkedin.android.litr.recorder.MediaRecordParameters
import com.linkedin.android.litr.recorder.MediaRecordRequestManager
import com.linkedin.android.litr.recorder.readers.SurfaceTrackReader
import com.linkedin.android.litr.render.GlVideoRenderer
import com.linkedin.android.litr.utils.TransformationUtil
import java.io.File
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class RecordSurfaceFragment : BaseTransformationFragment() {
    private lateinit var binding: FragmentRecordSurfaceBinding
    private lateinit var mediaRecorder: MediaRecordRequestManager

    private var requestId = UUID.randomUUID().toString()
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaRecorder = MediaRecordRequestManager(context!!.applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordSurfaceBinding.inflate(inflater, container, false)

        binding.recordButton.setOnClickListener {
            if (!isRecording) {
                activity?.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_LOCKED
                binding.recordButton.setText(R.string.stop)
                isRecording = true
                startTranscoding()
            } else {
                // Stop recording
                activity?.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                binding.recordButton.setText(R.string.record)
                isRecording = false
                stopTranscoding()
            }
        }

        return binding.root
    }

    private fun stopTranscoding() {
        mediaRecorder.stop(requestId)
    }

    private fun startTranscoding() {
        val targetFile = File(
            TransformationUtil.getTargetFileDirectory(requireContext().applicationContext),
            "recorded_surface_${System.currentTimeMillis()}.mp4"
        )

        val targetMedia = TargetMedia().apply {
            setTargetFile(targetFile)
        }

        val mediaTarget: MediaTarget = MediaMuxerMediaTarget(
            targetMedia.targetFile.path,
            1,
            0,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )

        val format = MediaFormat.createVideoFormat("video/avc", 1080, 1920).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate(1080, 1920))
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3)
            setLong(MediaFormat.KEY_DURATION, 60L * 1000L * 1000L)
        }

        val videoTrackParams = MediaRecordParameters(
            reader = surfaceTrackReader,
            // TODO: Obtain source format from screen parameters?
            sourceFormat = format,
            targetTrack = 0,
            targetFormat = format,
            mediaTarget = mediaTarget,
            encoder = MediaCodecEncoder(),
            renderer = GlVideoRenderer(null)
        )

        requestId = UUID.randomUUID().toString()
        mediaRecorder.record(
            requestId,
            listOf(videoTrackParams)
        )
    }

    // region: Event listeners

    private val surfaceTrackReader = object: SurfaceTrackReader {
        private val colors = listOf(0xFFDEA47E, 0xFFCD4631, 0xFFF8F2DC).map { it.toInt() }

        private var isStarted = false

        private var lastTime: Long? = null
        private var totalSec = 0.0

        private val circlePaint = Paint().apply {
            color = colors[1]
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        private val textPaint = TextPaint().apply {
            color = colors[2]
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 48f
            isFakeBoldText = true
        }

        private fun drawFrameInternal(surface: Surface, presentationTimeNs: Long) {
            val canvas = surface.lockCanvas(null)

            val deltaSec = (presentationTimeNs - (lastTime ?: presentationTimeNs)) / NANOS_PER_SECOND
            lastTime = presentationTimeNs
            totalSec += deltaSec

            val centerX = canvas.width * .5f + sin(totalSec).toFloat() * 100f
            val centerY = canvas.height * .5f + cos(totalSec).toFloat() * 100f

            canvas.drawColor(colors[0])
            canvas.drawCircle(centerX, centerY, canvas.width * .2f, circlePaint)
            canvas.drawText("%.3f".format(totalSec), centerX, centerY, textPaint)

            surface.unlockCanvasAndPost(canvas)
        }

        override fun drawFrame(surface: Surface, presentationTimeNs: Long) {
            if (!isStarted) return

            // Draw frame to the Surface to be encoded, offscreen.
            drawFrameInternal(surface, presentationTimeNs)

            // If we want to preview, also draw frame to the SurfaceView (for preview)
            drawFrameInternal(binding.previewSurface.holder.surface, presentationTimeNs)
        }

        override fun start() {
            lastTime = null
            totalSec = 0.0

            isStarted = true
        }

        override fun stop() {
            isStarted = false
        }
    }

    // endregion

    companion object {
        private val TAG = RecordSurfaceFragment::class.qualifiedName

        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val FRAME_RATE = 30
        private const val BPP = 0.25f

        fun calcBitRate(width: Int, height: Int): Int {
            val bitrate =
                (BPP * FRAME_RATE * width * height).toInt()
            Log.i(
                TAG,
                "bitrate=$bitrate"
            )
            return bitrate
        }
    }
}