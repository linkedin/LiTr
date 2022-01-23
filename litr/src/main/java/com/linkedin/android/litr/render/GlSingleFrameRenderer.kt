/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.render

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.Matrix
import android.view.Surface
import com.linkedin.android.litr.filter.GlFilter
import com.linkedin.android.litr.filter.GlFrameRenderFilter
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter
import java.nio.ByteBuffer
import java.util.*


/**
 * A renderer that applies OpenGL filters to a bitmap, and returns a new bitmap.
 */
class GlSingleFrameRenderer(filters: List<GlFilter>?) : SingleFrameRenderer {
    private val hasFilters: Boolean = filters != null && filters.isNotEmpty()

    private val filters: MutableList<GlFilter>
    private val stMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private lateinit var destFramebuffer: GlFramebuffer
    private lateinit var inputSize: Point
    private lateinit var inputSurface: VideoRenderInputSurface
    private lateinit var outputSurface: VideoRenderOutputSurface
    private lateinit var bitmapPaint: Paint
    private var pixelBuffer: ByteBuffer? = null
    private var isInitialized = false

    init {

        this.filters = ArrayList()
        if (filters == null) {
            this.filters.add(DefaultVideoFrameRenderFilter())
        } else {
            val hasFrameRenderFilter = filters.any { it is GlFrameRenderFilter }
            if (!hasFrameRenderFilter) {
                // if client provided filters don't have a frame render filter, insert default frame filter
                this.filters.add(DefaultVideoFrameRenderFilter())
            }
            this.filters.addAll(filters)
        }
    }

    private fun saveTexture(width: Int, height: Int): Bitmap {
        pixelBuffer?.rewind()
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer)
        val destBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        destBitmap.copyPixelsFromBuffer(pixelBuffer)
        return destBitmap
    }

    private fun init(width: Int, height: Int) {
        inputSize = Point(width, height)
        bitmapPaint = Paint()

        val capacity = width * height * 4
        pixelBuffer = ByteBuffer.allocate(capacity)

        Matrix.setIdentityM(stMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)

        // Creates a GL texture, and a SurfaceTexture. Since the rest of the pipeline (e.g. filters) rely on an external texture sampler, we will render the
        // source bitmap into this surface, instead of dealing with GL_TEXTURE_2D, so this surface be passed to the rest of the pipeline with no changes.
        inputSurface = VideoRenderInputSurface().apply {
            surfaceTexture.setDefaultBufferSize(inputSize.x, inputSize.y)
        }

        // The outputSurface is needed to set up EGL surface and context
        val surfaceTexture = SurfaceTexture(0).apply {
            setDefaultBufferSize(inputSize.x, inputSize.y)
        }
        outputSurface = VideoRenderOutputSurface(Surface(surfaceTexture))
        surfaceTexture.release()

        // Flips the geometry on the Y axis, to produce correct orientation in the bitmap
        Matrix.scaleM(mvpMatrix, 0, 1f, -1f, 1f)

        filters.forEach {
            it.init()
            it.setVpMatrix(mvpMatrix, 0)
        }

        val destTexture = GlTexture(GLES20.GL_TEXTURE0, GLES20.GL_TEXTURE_2D, null, inputSize.x, inputSize.y)
        destTexture.bind()
        destFramebuffer = GlFramebuffer()
        destFramebuffer.bind()
        destFramebuffer.attachTexture(destTexture.texName)
        destFramebuffer.unbind()
        destTexture.unbind()
    }

    override fun renderFrame(input: Bitmap?, presentationTimeNs: Long): Bitmap? {
        if (!hasFilters || input == null) {
            return input
        }

        if (!isInitialized) {
            init(input.width, input.height)
            isInitialized = true
        }

        // Produce frame on input surface
        val srcCanvas = inputSurface.surface.lockCanvas(null)
        srcCanvas.drawBitmap(input, 0f, 0f, bitmapPaint)
        inputSurface.surface.unlockCanvasAndPost(srcCanvas)

        // Await frame
        inputSurface.awaitNewImage()

        // Draw input frame into destination framebuffer object, read frame as bitmap
        destFramebuffer.bind()
        drawFilters(presentationTimeNs)
        val readBitmap = saveTexture(inputSize.x, inputSize.y)
        destFramebuffer.unbind()
        return readBitmap
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    private fun drawFilters(presentationTimeNs: Long) {
        inputSurface.getTransformMatrix(stMatrix)
        filters.mapNotNull { it as? GlFrameRenderFilter }.forEach { filter ->
            filter.initInputFrameTexture(inputSurface.textureId, stMatrix)
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
        for (filter in filters) {
            filter.apply(presentationTimeNs)
        }
        GLES20.glFinish()
    }

    fun release() {
        for (filter in filters) {
            filter.release()
        }
        inputSurface.release()
        outputSurface.release()
        destFramebuffer.delete()
    }

    fun hasFilters(): Boolean {
        return hasFilters
    }

}
