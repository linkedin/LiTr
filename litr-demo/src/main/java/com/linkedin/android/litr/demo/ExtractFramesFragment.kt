/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo

import android.R
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import com.linkedin.android.litr.demo.data.SourceMedia
import com.linkedin.android.litr.demo.databinding.FragmentExtractFramesBinding
import com.linkedin.android.litr.filter.GlFilter
import com.linkedin.android.litr.render.GlThumbnailRenderer
import com.linkedin.android.litr.thumbnails.*
import com.linkedin.android.litr.thumbnails.behaviors.MediaMetadataExtractionBehavior
import java.util.*
import java.util.concurrent.Executors

class ExtractFramesFragment : BaseTransformationFragment(), MediaPickerListener {
    private lateinit var binding: FragmentExtractFramesBinding
    private lateinit var filtersAdapter: ArrayAdapter<DemoFilter>
    private lateinit var thumbnailExtractor: VideoThumbnailExtractor
    private var requestId: String = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filtersAdapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, DemoFilter.values()).apply {
            setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        }

        thumbnailExtractor = VideoThumbnailExtractor(
            Executors.newSingleThreadExecutor(),
            object : ThumbnailExtractListener {
                override fun onStarted(id: String, timestampsUs: List<Long>) {
                    val bitmaps: MutableList<Bitmap?> = ArrayList(timestampsUs.size)
                    for (i in timestampsUs.indices) {
                        bitmaps.add(null)
                    }
                    binding.filmStripTimeline.setFrameList(bitmaps)
                }

                override fun onExtracted(id: String, index: Int, bitmap: Bitmap) {
                    binding.filmStripTimeline.setFrameAt(index, bitmap)
                }

                override fun onExtractFrameFailed(id: String, index: Int) {
                }

                override fun onCompleted(id: String) {}
                override fun onCancelled(id: String) {}
                override fun onError(id: String, cause: Throwable?) {}
            }, Looper.getMainLooper()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        thumbnailExtractor.release()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentExtractFramesBinding.inflate(inflater, container, false).also { binding ->
            binding.fragment = this@ExtractFramesFragment
            binding.sourceMedia = SourceMedia()
            binding.sectionPickVideo.buttonPickVideo.setOnClickListener { pickVideo(this@ExtractFramesFragment) }
            binding.spinnerFilters.adapter = filtersAdapter
            binding.spinnerFilters.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                    binding.filter = filtersAdapter.getItem(position)?.filter
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        return binding.root
    }

    fun extractThumbnails(sourceMedia: SourceMedia, filter: GlFilter?) {

        thumbnailExtractor.stop(requestId)

        val thumbCount = 10

        val videoDurationSec = sourceMedia.duration.toDouble()
        val timelineWidth = binding.filmStripTimeline.width
        val thumbWidth = timelineWidth / thumbCount
        val thumbHeight = binding.filmStripTimeline.height

        if (videoDurationSec <= 0 || timelineWidth <= 0 || thumbHeight <= 0) {
            return
        }

        val secPerThumbnail = videoDurationSec / thumbCount
        val timestamps = (0 until thumbCount).map {
            (secPerThumbnail * 1000000.0).toLong() * it
        }

        val retriever = MediaMetadataRetriever().apply {
            setDataSource(requireContext(), sourceMedia.uri)
        }

        val params = ThumbnailExtractParameters(
            MediaMetadataExtractionBehavior(retriever, ExtractionMode.TwoPass),
            timestamps,
            Point(thumbWidth, thumbHeight),
            GlThumbnailRenderer(filter?.let { listOf(it) })
        )

        requestId = UUID.randomUUID().toString()
        thumbnailExtractor.extract(requestId, params)
    }

    override fun onMediaPicked(uri: Uri) {
        binding.sourceMedia?.let {
            updateSourceMedia(it, uri)
        }
    }
}
