/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl.shader;

public class VertexShader {

    public static final String THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +

            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +

            "uniform highp float texelWidth;\n" +
            "uniform highp float texelHeight;\n" +

            "varying highp vec2 textureCoordinate;\n" +
            "varying highp vec2 leftTextureCoordinate;\n" +
            "varying highp vec2 rightTextureCoordinate;\n" +

            "varying highp vec2 topTextureCoordinate;\n" +
            "varying highp vec2 topLeftTextureCoordinate;\n" +
            "varying highp vec2 topRightTextureCoordinate;\n" +

            "varying highp vec2 bottomTextureCoordinate;\n" +
            "varying highp vec2 bottomLeftTextureCoordinate;\n" +
            "varying highp vec2 bottomRightTextureCoordinate;\n" +

            "void main()\n" +
            "{\n" +
            "gl_Position = uMVPMatrix * aPosition;\n" +

            "vec2 widthStep = vec2(texelWidth, 0.0);\n" +
            "vec2 heightStep = vec2(0.0, texelHeight);\n" +
            "vec2 widthHeightStep = vec2(texelWidth, texelHeight);\n" +
            "vec2 widthNegativeHeightStep = vec2(texelWidth, -texelHeight);\n" +

            "textureCoordinate = (uSTMatrix * aTextureCoord).xy;\n" +
            "leftTextureCoordinate = textureCoordinate - widthStep;\n" +
            "rightTextureCoordinate = textureCoordinate + widthStep;\n" +

            "topTextureCoordinate = textureCoordinate - heightStep;\n" +
            "topLeftTextureCoordinate = textureCoordinate - widthHeightStep;\n" +
            "topRightTextureCoordinate = textureCoordinate + widthNegativeHeightStep;\n" +

            "bottomTextureCoordinate = textureCoordinate + heightStep;\n" +
            "bottomLeftTextureCoordinate = textureCoordinate - widthNegativeHeightStep;\n" +
            "bottomRightTextureCoordinate = textureCoordinate + widthHeightStep;\n" +
            "}";
}
