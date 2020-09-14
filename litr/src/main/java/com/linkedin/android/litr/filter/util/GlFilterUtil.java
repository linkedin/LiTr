/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.util;

import android.opengl.Matrix;

import androidx.annotation.NonNull;

import com.linkedin.android.litr.filter.Transform;

public class GlFilterUtil {

    /**
     * Takes target video VP matrix, along with filter rectangle parameters (size, position, rotation)
     * and calculates filter's MVP matrix
     * @param vpMatrix target video VP matrix, which defines target video canvas
     * @param transform {@link Transform} that defines drawable's positioning within target video frame
     * @return filter MVP matrix
     */
    @NonNull
    public static float[] createFilterMvpMatrix(@NonNull float[] vpMatrix,
                                                @NonNull Transform transform) {
        // Let's use features of VP matrix to extract frame aspect ratio and orientation from it
        // for 90 and 270 degree rotations (portrait orientation) top left element will be zero
        boolean isPortraitVideo = vpMatrix[0] == 0;

        // orthogonal projection matrix is basically a scaling matrix, which scales along X axis.
        // 0 and 180 degree rotations keep the scaling factor in top left element (they don't move it)
        // 90 and 270 degree rotations move it to one position right in top row
        // Inverting scaling factor gives us the aspect ratio.
        // Scale can be negative if video is flipped, so we use absolute value.
        float videoAspectRatio;
        if (isPortraitVideo) {
            videoAspectRatio = 1 / Math.abs(vpMatrix[4]);
        } else {
            videoAspectRatio = 1 / Math.abs(vpMatrix[0]);
        }

        // Size is respective to video frame, and frame will later be scaled by perspective and view matrices.
        // So we have to adjust the scale accordingly.
        float scaleX;
        float scaleY;
        if (isPortraitVideo) {
            scaleX = transform.size.x;
            scaleY = transform.size.y * videoAspectRatio;
        } else {
            scaleX = transform.size.x * videoAspectRatio;
            scaleY = transform.size.y;
        }

        // Position values are in relative (0, 1) range, which means they have to be mapped from (-1, 1) range
        // and adjusted for aspect ratio.
        float translateX;
        float translateY;
        if (isPortraitVideo) {
            translateX = transform.position.x * 2 - 1;
            translateY = (1 - transform.position.y * 2) * videoAspectRatio;
        } else {
            translateX = (transform.position.x * 2 - 1) * videoAspectRatio;
            translateY = 1 - transform.position.y * 2;
        }

        // Matrix operations in OpenGL are done in reverse. So here we scale (and flip vertically) first, then rotate
        // around the center, and then translate into desired position.
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, translateX, translateY, 0);
        Matrix.rotateM(modelMatrix, 0, transform.rotation, 0, 0, 1);
        Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1);

        // last, we multiply the model matrix by the view matrix to get final MVP matrix for an overlay
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        return mvpMatrix;
    }
}
