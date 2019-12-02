/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 42;
    private static final String MAIN_FRAGMENT = "fragment";

    private Fragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (savedInstanceState != null) {
            mainFragment = fragmentManager.getFragment(savedInstanceState, MAIN_FRAGMENT);
        } else {
            mainFragment = new MainFragment();
        }
        fragmentManager.beginTransaction().replace(R.id.fragment_container, mainFragment).commit();

        if (!hasWriteExternalStoragePermission()) {
            ActivityCompat.requestPermissions(MainActivity.this,
                                              new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },
                                              REQUEST_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!hasWriteExternalStoragePermission()) {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // TODO implement these for pre-Marshmallow devices, if possible
            menu.removeItem(R.id.codec_list);
            menu.removeItem(R.id.avc_encoders);
            menu.removeItem(R.id.avc_decoders);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(this, InfoActivity.class);
        switch (item.getItemId()) {
            case R.id.device_info:
                intent.setAction(InfoActivity.ACTION_DISPLAY_DEVICE_INFO);
                break;
            case R.id.capture_formats:
                intent.setAction(InfoActivity.ACTION_DISPLAY_CAPTURE_FORMATS);
                break;
            case R.id.codec_list:
                intent.setAction(InfoActivity.ACTION_DISPLAY_CODEC_LIST);
                break;
            case R.id.avc_encoders:
                intent.setAction(InfoActivity.ACTION_DISPLAY_AVC_ENCODERS);
                break;
            case R.id.avc_decoders:
                intent.setAction(InfoActivity.ACTION_DISPLAY_AVC_DECODERS);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        startActivity(intent);

        return true;
    }

    private boolean hasWriteExternalStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}
