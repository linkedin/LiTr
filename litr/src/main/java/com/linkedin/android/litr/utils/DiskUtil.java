/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class DiskUtil {
    private static final String TAG = DiskUtil.class.getSimpleName();

    public static final long FREE_DISK_SPACE_CHECK_FAILED = -1;

    /**
     * This method returns the available disk space in data directory, measured in bytes,
     * for the application.
     *
     * @return free disk space in bytes, or FREE_DISK_SPACE_CHECK_FAILED if cannot be determined.
     */
    @SuppressWarnings("IllegalCatch")
    public long getAvailableDiskSpaceInDataDirectory() {
        try {
            StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                return statFs.getAvailableBytes();
            } else {
                return (long) statFs.getAvailableBlocks() * statFs.getBlockSize();
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not get Available Disk Space");
            return FREE_DISK_SPACE_CHECK_FAILED;
        }
    }

}
