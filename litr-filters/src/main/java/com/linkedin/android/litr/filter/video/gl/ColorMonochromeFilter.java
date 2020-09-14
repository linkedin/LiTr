package com.linkedin.android.litr.filter.video.gl;

import android.graphics.PointF;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

import java.nio.FloatBuffer;

/**
 * Created by tapos-datta on 9/14/20.
 */
public class ColorMonochromeFilter extends BaseFrameRenderFilter{

    private static final String FRAGMENT_SHADER =
                    "#extension GL_OES_EGL_image_external : require\n" +

                    "precision mediump float;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "uniform lowp samplerExternalOES sTexture;" +
                    "uniform lowp vec3 newColor;" +
                    "uniform lowp float intensity;\n" +
                    "\n"+
                    "const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);" +
                    "\n" +
                    "void main() {" +
                    "   lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);" +
                    "   float luminance = dot(textureColor.rgb, luminanceWeighting);" +
                    "   lowp vec4 desat = vec4(vec3(luminance), 1.0);" +
                    "   lowp vec4 outputColor = vec4(" +
                    "   (desat.r < 0.5 ? (2.0 * desat.r * newColor.r) : (1.0 - 2.0 * (1.0 - desat.r) * (1.0 - newColor.r)))," +
                    "   (desat.g < 0.5 ? (2.0 * desat.g * newColor.g) : (1.0 - 2.0 * (1.0 - desat.g) * (1.0 - newColor.g)))," +
                    "   (desat.b < 0.5 ? (2.0 * desat.b * newColor.b) : (1.0 - 2.0 * (1.0 - desat.b) * (1.0 - newColor.b)))," +
                    "   1.0" +
                    "   );" +
                    "   gl_FragColor = vec4(mix(textureColor.rgb, outputColor.rgb, intensity), textureColor.a);" +
                    "}";

    private float intensity;
    private float[] inputColorRGB;

    /**
     * Create the instance frame render filter
     * @param inputColorRGB contains the color information (i.e. r,g,b) from 0.0 to 1.0
     * @param intensity value, from range 0.0 to 1.0;
     */
    public ColorMonochromeFilter(float[] inputColorRGB,float intensity) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.intensity = intensity;
        this.inputColorRGB = inputColorRGB;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param inputColorRGB contains the color information (i.e. r,g,b) from 0.0 to 1.0
     * @param intensity value, from range 0.0 to 1.0;
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public ColorMonochromeFilter(float[] inputColorRGB, float intensity, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, size, position, rotation);

        this.intensity = intensity;
        this.inputColorRGB = inputColorRGB;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("intensity"), intensity);
        GLES20.glUniform3fv(getHandle("newColor"),1, FloatBuffer.wrap(inputColorRGB));
    }
}
