package com.linkedin.android.litr.render

import android.opengl.GLES20

class GlTexture @JvmOverloads constructor(
    val unit: Int = GLES20.GL_TEXTURE0,
    val target: Int = GLES20.GL_TEXTURE_2D,
    id: Int? = null,
    val width: Int = 0,
    val height: Int = 0
) {
    val texName: Int = if (id == null) {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textures[0]
    } else {
        id
    }

    init {
        GLES20.glBindTexture(target, texName)
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

        GLES20.glBindTexture(target, 0)
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
