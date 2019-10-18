/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;
import com.linkedin.android.litr.utils.DeviceUtil;

public class InfoActivity extends AppCompatActivity {
    static final String ACTION_DISPLAY_DEVICE_INFO = "com.linkedin.android.litr.demo.DISPLAY_DEVICE_INFO";
    static final String ACTION_DISPLAY_CAPTURE_FORMATS = "com.linkedin.android.litr.demo.DISPLAY_CAPTURE_FORMATS";
    static final String ACTION_DISPLAY_CODEC_LIST = "com.linkedin.android.litr.demo.DISPLAY_CODEC_LIST";
    static final String ACTION_DISPLAY_AVC_ENCODERS = "com.linkedin.android.litr.demo.DISPLAY_AVC_ENCODERS";
    static final String ACTION_DISPLAY_AVC_DECODERS = "com.linkedin.android.litr.demo.DISPLAY_AVC_DECODERS";

    private TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        info = findViewById(R.id.info);

        String action = getIntent().getAction();
        if (action == null) {
            return;
        }

        String title = null;
        switch (action) {
            case ACTION_DISPLAY_DEVICE_INFO:
                title = getString(R.string.menu_device_info);
                printDeviceInfo();
                break;
            case ACTION_DISPLAY_CAPTURE_FORMATS:
                title = getString(R.string.menu_capture_formats);
                printCaptureFormats();
                break;
            case ACTION_DISPLAY_CODEC_LIST:
                title = getString(R.string.menu_codecs);
                printCodecList();
                break;
            case ACTION_DISPLAY_AVC_ENCODERS:
                title = getString(R.string.menu_avc_encoders);
                printAvcEncoderCapabilities();
                break;
            case ACTION_DISPLAY_AVC_DECODERS:
                title = getString(R.string.menu_avc_decoders);
                printAvcDecoderCapabilities();
                break;
            default:
                Toast.makeText(this, "Unsupported info display request received", Toast.LENGTH_SHORT).show();
        }

        ActionBar actionBar = getSupportActionBar();
        if (title != null && actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    private void printDeviceInfo() {
        info.setText(DeviceUtil.getDeviceInfo(this));
    }

    private void printCaptureFormats() {
        info.setText(DeviceUtil.getCaptureFormats(this));
    }

    private void printCodecList() {
        info.setText(DeviceUtil.getCodecList());
    }

    private void printAvcDecoderCapabilities() {
        info.setText(DeviceUtil.getAvcDecoderCapabilities(this));
    }

    private void printAvcEncoderCapabilities() {
        info.setText(DeviceUtil.getAvcEncoderCapabilities(this));
    }
}
