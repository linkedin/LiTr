package com.linkedin.android.litr.filter.video.gl;

import android.graphics.PointF;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

/**
 * Created by tapos-datta on 9/14/20.
 */
public class ColorMatrixFilter extends BaseFrameRenderFilter{

    private static final String FRAGMENT_SHADER =
                    "#extension GL_OES_EGL_image_external : require\n" +

                    "precision mediump float;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "uniform lowp samplerExternalOES sTexture;" +
                    "uniform lowp mat4 matrix;" +
                    "uniform lowp float intensity;\n" +
                    "\n" +
                    "void main(){\n" +
                    "    vec4 textureColor = texture2D(sTexture,vTextureCoord);\n" +
                    "    vec4 outputColor = textureColor * matrix;\n" +
                    "    gl_FragColor = mix(textureColor, outputColor, intensity);\n" +
                    "}";


    private float intensity;
    private float[] colorMatrix4x4; // size = 16; (i.e. 4 x 4)

    /**
     * Create ColorMatrix frame render filter
     * @param colorMatrix4x4  contains the color information (i.e. r,g,b,a) from 0.0 to 1.0
     * @param intensity value, from range 0.0 to 1.0;
     */
    public ColorMatrixFilter(float[] colorMatrix4x4,float intensity) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.intensity = intensity;
        this.colorMatrix4x4 = colorMatrix4x4;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param colorMatrix4x4 matrix of 4x4, contains the color information (i.e. r,g,b,a) from 0.0 to 1.0
     * @param intensity value, from range 0.0 to 1.0;
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public ColorMatrixFilter(float[] colorMatrix4x4,float intensity, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, size, position, rotation);

        this.intensity = intensity;
        this.colorMatrix4x4 = colorMatrix4x4;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("intensity"), intensity);
        GLES20.glUniformMatrix4fv(getHandle("matrix"), 1, false, colorMatrix4x4, 0);
    }
}
