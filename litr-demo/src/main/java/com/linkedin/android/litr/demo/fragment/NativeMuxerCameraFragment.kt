package com.linkedin.android.litr.demo.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.linkedin.android.litr.demo.RecordCamera2Fragment

@RequiresApi(Build.VERSION_CODES.M)
class NativeMuxerCameraFragment: RecordCamera2Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedInstanceState).also {
            binding.enableNativeMuxer = true
        }
    }
}