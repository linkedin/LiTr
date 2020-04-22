package com.linkedin.android.litr.demo.data;

import android.view.View;
import android.widget.AdapterView;

import androidx.databinding.BaseObservable;

public class TargetVideoConfiguration extends BaseObservable {

    public int rotation;

    public void onRotationSelected(AdapterView<?> parent, View view, int pos, long id) {
        switch (pos) {
            case 0:
                // landscape
                rotation = 0;
                break;
            case 1:
                // portrait
                rotation = 90;
                break;
            case 2:
                // reverse landscape
                rotation = 180;
                break;
            case 3:
                // reverse portrait
                rotation = 270;
                break;
            default:
                // nor handled yet
        }
    }
}
