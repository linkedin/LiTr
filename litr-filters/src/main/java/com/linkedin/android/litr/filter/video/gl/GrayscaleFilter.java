/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl;

import com.linkedin.android.litr.shader.FragmentShaders;
import com.linkedin.android.litr.shader.VertexShaders;

public class GrayscaleFilter extends GlVideoFrameRenderFilter {

    public GrayscaleFilter() {
        super(VertexShaders.DEFAULT_SHADER, FragmentShaders.GRAYSCALE_SHADER);
    }
}
