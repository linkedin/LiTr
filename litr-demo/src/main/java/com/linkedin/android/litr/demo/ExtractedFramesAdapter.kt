/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.linkedin.android.litr.ExperimentalFrameExtractorApi
import com.linkedin.android.litr.frameextract.FrameExtractListener
import com.linkedin.android.litr.frameextract.FrameExtractParameters
import com.linkedin.android.litr.frameextract.VideoFrameExtractor
import java.util.*

@OptIn(ExperimentalFrameExtractorApi::class)
class ExtractedFramesAdapter(private val extractor: VideoFrameExtractor, private val cache: LruCache<Long, ByteArray>) :
        RecyclerView.Adapter<ExtractedFramesAdapter.FrameViewHolder>() {

    private val frames = mutableListOf<FrameExtractParameters>()

    fun loadData(frames: List<FrameExtractParameters>) {
        this.frames.clear()
        this.frames.addAll(frames)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_frame, parent, false)

        return FrameViewHolder(view, null)
    }

    override fun onBindViewHolder(holder: FrameViewHolder, position: Int) {

        val frameParams = frames[position]

        val requestId = UUID.randomUUID().toString()

        holder.requestId = requestId
        holder.imageView.visibility = View.GONE
        holder.indicator.setBackgroundColor(Color.GRAY)

        val cachedByteArray = cache.get(frameParams.timestampUs)
        if (cachedByteArray != null) {
            // Cache hit: just use the cached bitmap.
            val cachedBitmap = BitmapFactory.decodeByteArray(cachedByteArray, 0, cachedByteArray.size)
            holder.imageView.setImageBitmap(cachedBitmap)
            holder.imageView.visibility = View.VISIBLE
            holder.indicator.setBackgroundColor(Color.BLACK)
        } else {
            // Cache miss: start the frame extraction job.
            extractor.extract(
                    requestId,
                    frameParams,
                    object : FrameExtractListener {
                        override fun onStarted(id: String, timestampUs: Long) {
                            holder.indicator.setBackgroundColor(Color.BLUE)
                        }

                        override fun onExtracted(id: String, timestampUs: Long, bitmap: Bitmap) {
                            holder.imageView.setImageBitmap(bitmap)
                            holder.imageView.visibility = View.VISIBLE
                            holder.indicator.setBackgroundColor(Color.GREEN)
                        }

                        override fun onCancelled(id: String, timestampUs: Long) {
                            holder.indicator.setBackgroundColor(Color.YELLOW)
                        }

                        override fun onError(id: String, timestampUs: Long, cause: Throwable?) {
                            holder.indicator.setBackgroundColor(Color.RED)

                        }
                    })
        }
    }

    override fun onViewRecycled(holder: FrameViewHolder) {
        holder.requestId?.let {
            extractor.stop(it)
        }
        holder.indicator.setBackgroundColor(Color.MAGENTA)
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int {
        return frames.size
    }

    class FrameViewHolder(itemView: View, var requestId: String?) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = this.itemView.findViewById(R.id.frameImageView)
        val indicator: View = this.itemView.findViewById(R.id.debugIndicator)
    }
}
