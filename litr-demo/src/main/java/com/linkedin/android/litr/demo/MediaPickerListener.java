/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.net.Uri;
import androidx.annotation.NonNull;

public interface MediaPickerListener {

    void onMediaPicked(@NonNull Uri uri);
}
