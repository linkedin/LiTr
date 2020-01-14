/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.databinding.InverseMethod;

public class Converter {

    @InverseMethod("stringToInteger")
    public static String integerToString(int value) {
        return Integer.toString(value);
    }

    public static int stringToInteger(@NonNull String string) {
        if (!TextUtils.isEmpty(string)) {
            return Integer.parseInt(string);
        }
        return 0;
    }

    @InverseMethod("stringToVideoBitrate")
    public static String videoBitrateToString(int bitrate) {
        if (bitrate > 0) {
            return Float.toString((float) bitrate / 1000000);
        }
        return Integer.toString(bitrate);
    }

    public static int stringToVideoBitrate(@NonNull String string) {
        if (!TextUtils.isEmpty(string)) {
            return (int) (Float.parseFloat(string) * 1000000);
        }
        return 0;
    }

    @InverseMethod("stringToAudioBitrate")
    public static String audioBitrateToString(int bitrate) {
        return Integer.toString(bitrate / 1000);
    }

    public static int stringToAudioBitrate(@NonNull String string) {
        if (!TextUtils.isEmpty(string)) {
            return Integer.parseInt(string) * 1000;
        }
        return 0;
    }
}
