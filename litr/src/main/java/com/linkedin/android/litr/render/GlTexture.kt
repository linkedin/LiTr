package com.linkedin.android.litr.render

import android.graphics.Bitmap
import android.opengl.GLES20
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


class GlTexture @JvmOverloads constructor(val unit: Int = GLES20.GL_TEXTURE0, val target: Int = GLES20.GL_TEXTURE_2D, id: Int? = null, val width: Int = 0, val height: Int = 0) {
    val texName: Int = if (id == null) {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textures[0]
    } else {
        id
    }

    init {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texName)
        GlRenderUtils.checkGlError("glBindTexture GlTexture")

        if (width > 0 && height > 0) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
            GlRenderUtils.checkGlError("glTexImage2D GlTexture")
        }

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GlRenderUtils.checkGlError("glTexParameter GlTexture")

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }


    fun bind() {
        GLES20.glActiveTexture(unit)
        GLES20.glBindTexture(target, texName)
        GlRenderUtils.checkGlError("glBindTexture GlTexture")
    }

    fun unbind() {
        GLES20.glBindTexture(target, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    }

    fun delete() {
        GLES20.glDeleteTextures(1, intArrayOf(texName), 0)
    }
}


fun surfaceToBitmap(width: Int, height: Int): Bitmap {
    // read image pixels from the gl context
    val buf = ByteBuffer.allocateDirect(width * height * 4)
    buf.order(ByteOrder.LITTLE_ENDIAN)
    GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
//    Egloo.checkGlError("glReadPixels")
    buf.rewind()

    // returned buffer is upside down - so we need to reverse it line by line
    val chunkSize = width * 4
    val byteStream = ByteArrayOutputStream(chunkSize)
    val copy = ByteArray(chunkSize)
    for (i in height - 1 downTo 0) {
        buf.position(i * chunkSize)
        buf.get(copy, 0, chunkSize)
        byteStream.write(copy)
    }
    val upsideDownBuf = ByteBuffer.wrap(byteStream.toByteArray())

    // generate bitmap, copy buffer to it
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(upsideDownBuf)

    return bitmap
}