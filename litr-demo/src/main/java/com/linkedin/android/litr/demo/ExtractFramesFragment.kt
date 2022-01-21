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
import android.net.Uri
import android.os.Bundle
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.linkedin.android.litr.demo.data.SourceMedia
import com.linkedin.android.litr.demo.databinding.FragmentExtractFramesBinding
import com.linkedin.android.litr.filter.GlFilter
import com.linkedin.android.litr.render.GlThumbnailRenderer
import com.linkedin.android.litr.thumbnails.ExtractionMode
import com.linkedin.android.litr.thumbnails.ThumbnailExtractListener
import com.linkedin.android.litr.thumbnails.ThumbnailExtractParameters
import com.linkedin.android.litr.thumbnails.VideoThumbnailExtractor
import java.util.*

class ExtractFramesFragment : BaseTransformationFragment(), MediaPickerListener {
    private lateinit var binding: FragmentExtractFramesBinding
    private lateinit var filtersAdapter: ArrayAdapter<DemoFilter>
    private lateinit var thumbnailExtractor: VideoThumbnailExtractor
    private lateinit var framesAdapter: ExtractedFramesAdapter
    private lateinit var bitmapInMemoryCache: LruCache<Long, Bitmap>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cacheSize = (Runtime.getRuntime().maxMemory() / 1024L) / 8L
        bitmapInMemoryCache = object : LruCache<Long, Bitmap>(cacheSize.toInt()) {
            override fun sizeOf(key: Long, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }

        filtersAdapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, DemoFilter.values()).apply {
            setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        }

        thumbnailExtractor = VideoThumbnailExtractor(requireContext())
        framesAdapter = ExtractedFramesAdapter(thumbnailExtractor, bitmapInMemoryCache)
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
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    filtersAdapter.getItem(position)?.filter?.let {
                        binding.filter = it
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        binding.framesRecycler.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.framesRecycler.adapter = framesAdapter
        return binding.root
    }

    override fun onDestroyView() {
        binding.framesRecycler.adapter = null
        bitmapInMemoryCache.evictAll()
        super.onDestroyView()
    }

    fun extractThumbnails(sourceMedia: SourceMedia, filter: GlFilter?) {
        thumbnailExtractor.stopAll()
        bitmapInMemoryCache.evictAll()

        val thumbCount = 50

        val videoDurationSec = sourceMedia.duration.toDouble()
        val thumbHeight = binding.framesRecycler.height

        if (videoDurationSec <= 0 || thumbHeight <= 0) {
            return
        }

        // Based on the video duration and number of thumbnails, obtain the list of timestamps (in microseconds),
        // of each frame we should extract.
        val secPerThumbnail = videoDurationSec / thumbCount
        val timestamps = (0 until thumbCount).map {
            (secPerThumbnail * 1000000.0).toLong() * it
        }

        // Because we want to apply the same filter to each frame, the renderer may be shared between all thumbnail requests.
        val renderer = GlThumbnailRenderer(filter?.let { listOf(it) })

        // From each timestamp, construct the parameters to send to the thumbnail extractor.
        val params = timestamps.map {
            ThumbnailExtractParameters(
                sourceMedia.uri,
                it,
                renderer,
                ExtractionMode.Fast,
                Point(thumbHeight, thumbHeight),
                0L
            )
        }

        // Set the list of extraction params to the adapter. Note that frame extraction will only start when the adapter binds an item to a view.
        framesAdapter.loadData(params)

        // What follows is an optimization.
        // We request all the thumbnails with a low priority, and cache the resulting bitmaps in an in-memory cache.
        // Because of the lower priority, these frames will only load when the RecyclerView adapter is not requesting frames at a higher priority.
        // In a production application, an on-disk bitmap cache may be preferred.
        params.forEach {
            thumbnailExtractor.extract(UUID.randomUUID().toString(), it.copy(priority = 100L), object: ThumbnailExtractListener {
                override fun onExtracted(id: String, timestampUs: Long, bitmap: Bitmap) {
                    bitmapInMemoryCache.put(timestampUs, bitmap)
                }
            })
        }
    }

    override fun onMediaPicked(uri: Uri) {
        binding.sourceMedia?.let {
            updateSourceMedia(it, uri)
        }
    }
}
