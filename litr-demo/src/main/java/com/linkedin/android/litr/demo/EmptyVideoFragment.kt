package com.linkedin.android.litr.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.demo.data.SourceMedia
import com.linkedin.android.litr.demo.data.TargetMedia
import com.linkedin.android.litr.demo.data.TransformationPresenter
import com.linkedin.android.litr.demo.data.TransformationState
import com.linkedin.android.litr.demo.data.VideoTrackFormat
import com.linkedin.android.litr.demo.databinding.FragmentEmptyVideoBinding
import com.linkedin.android.litr.utils.TransformationUtil
import java.io.File

private const val DURATION = 5_000_000L

class EmptyVideoFragment : BaseTransformationFragment() {

    private lateinit var binding: FragmentEmptyVideoBinding

    private lateinit var mediaTransformer: MediaTransformer
    private var targetMedia: TargetMedia = TargetMedia()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaTransformer = MediaTransformer(context!!.applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaTransformer.release()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEmptyVideoBinding.inflate(inflater, container, false)

        // mimic portrait 720p video with no audio
        val sourceMedia = SourceMedia()
        sourceMedia.duration = DURATION.toFloat()
        val trackFormat = VideoTrackFormat(0, "video/avc")
        trackFormat.width = 1280
        trackFormat.height = 720
        trackFormat.rotation = 90
        trackFormat.frameRate = 30
        trackFormat.bitrate = 5_000_000
        trackFormat.keyFrameInterval = 3
        trackFormat.duration = DURATION
        sourceMedia.tracks.add(trackFormat)

        binding.sourceMedia = sourceMedia
        binding.buttonPickBackground.setOnClickListener {
            pickBackground { uri ->
                targetMedia.backgroundImageUri = uri
            }
        }
        binding.buttonPickVideoOverlay.setOnClickListener {
            pickOverlay { uri ->
                targetMedia.setOverlayImageUri(uri)
            }
        }
        binding.transformationState = TransformationState()
        binding.transformationPresenter = TransformationPresenter(context!!, mediaTransformer)

        val targetFile = File(TransformationUtil.getTargetFileDirectory(requireContext().applicationContext), "empty_video.mp4")
        targetMedia.setTargetFile(targetFile)
        targetMedia.setTracks(sourceMedia.tracks)

        binding.targetMedia = targetMedia

        return binding.root
    }
}