package com.linkedin.android.litr.recorder

interface MediaRecorder {
    fun stop()
    fun start()
    fun processNextFrame(): Int
}