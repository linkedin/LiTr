package com.linkedin.android.litr.demo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.linkedin.android.litr.demo.MediaPickerListener

class NativeMuxerTranscodeFragment: TranscodeVideoGlFragment(), MediaPickerListener {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState).also {
            binding.enableNativeMuxer = true
        }
    }
}