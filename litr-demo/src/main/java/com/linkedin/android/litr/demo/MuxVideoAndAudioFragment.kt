package com.linkedin.android.litr.demo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.demo.data.SourceMedia
import com.linkedin.android.litr.demo.data.TargetMedia
import com.linkedin.android.litr.demo.data.TransformationPresenter
import com.linkedin.android.litr.demo.data.TransformationState
import com.linkedin.android.litr.demo.databinding.FragmentMuxVideoAudioBinding
import com.linkedin.android.litr.utils.TransformationUtil
import java.io.File

class MuxVideoAndAudioFragment : BaseTransformationFragment() {

    private lateinit var binding: FragmentMuxVideoAudioBinding

    private lateinit var mediaTransformer: MediaTransformer
    private var targetMedia: TargetMedia = TargetMedia()

    private val mediaPickerListener = MediaPickerListenerImpl()

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
        binding = FragmentMuxVideoAudioBinding.inflate(inflater, container, false)

        binding.sourceVideo = SourceMedia()
        binding.sourceAudio = SourceMedia()

        binding.sectionPickVideo.buttonPickVideo.setOnClickListener {
            pickVideo(mediaPickerListener)
        }
        binding.sectionPickAudio.buttonPickAudio.setOnClickListener {
            pickAudio(mediaPickerListener)
        }
        binding.transformationState = TransformationState()
        binding.transformationPresenter = TransformationPresenter(context!!, mediaTransformer)

        binding.targetMedia = targetMedia

        return binding.root
    }

    private inner class MediaPickerListenerImpl : MediaPickerListener {
        override fun onMediaPicked(uri: Uri) {
            context?.contentResolver?.getType(uri)?.let { mimeType ->
                when {
                    mimeType.startsWith("video") -> {
                        updateSourceMedia(binding.sourceVideo!!, uri)
                        val targetFile = File(TransformationUtil.getTargetFileDirectory(requireContext().applicationContext),
                            "transcoded_" + TransformationUtil.getDisplayName(context!!, uri))
                        binding.targetMedia?.setTargetFile(targetFile)
                    }
                    mimeType.startsWith("audio") -> {
                        updateSourceMedia(binding.sourceAudio!!, uri)
                    }
                    else -> {
                        // do nothing, we don't mux non-AV tracks
                    }
                }
            }
            binding.transformationState?.setState(TransformationState.STATE_IDLE)
            binding.transformationState?.setStats(null)
        }
    }
}
