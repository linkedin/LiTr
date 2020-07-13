package com.linkedin.android.litr.shader;

public class FragmentShaders {

    public static final String DEFAULT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +      // highp here doesn't seem to matter
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    public static final String GRAYSCALE_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "const highp vec3 weight = vec3(0.2125, 0.7154, 0.0721);\n" +
            "void main() {\n" +
            "  float luminance = dot(texture2D(sTexture, vTextureCoord).rgb, weight);\n" +
            "  gl_FragColor = vec4(vec3(luminance), 1.0);\n" +
            "}\n";

    private FragmentShaders() {
    }
}
