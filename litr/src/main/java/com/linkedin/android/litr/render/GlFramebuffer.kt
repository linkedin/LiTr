package com.linkedin.android.litr.render

import android.opengl.GLES20

class GlFramebuffer {
    val id: Int

    init {
        val array = IntArray(1)
        GLES20.glGenFramebuffers(1, array, 0)
        GlRenderUtils.checkGlError("glGenFramebuffers GlFramebuffer")
        id = array[0]
    }

    @JvmOverloads
    fun attachTexture(textureId: Int, texTarget: Int = GLES20.GL_TEXTURE_2D, attachment: Int = GLES20.GL_COLOR_ATTACHMENT0, level: Int = 0) {
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            attachment,
            texTarget,
            textureId,
            level
        )
        GlRenderUtils.checkGlError("glFramebufferTexture2D GlFramebuffer")
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Bad status glCheckFramebufferStatus: $status")
        }
    }

    fun bind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id)
    }

    fun unbind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun delete() {
        GLES20.glDeleteFramebuffers(1, intArrayOf(id), 0)
    }
}