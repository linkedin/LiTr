package com.linkedin.android.litr.demo.camera

interface CameraThreadListener {
    fun onCameraStarted(previewWidth: Int, previewHeight: Int)
    fun onCameraStopped()
}