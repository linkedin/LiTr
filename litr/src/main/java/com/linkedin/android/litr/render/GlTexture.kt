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
