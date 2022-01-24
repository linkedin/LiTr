/*
* MIT License
*
* Copyright (c) 2019 Mattia Iavarone
* Copyright (c) 2021 LinkedIn Corporation
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
// modified from: https://github.com/natario1/Egloo
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
