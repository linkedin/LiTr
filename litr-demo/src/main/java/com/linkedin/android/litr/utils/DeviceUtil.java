/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;

import androidx.annotation.NonNull;

import com.linkedin.android.litr.demo.R;

import java.util.Locale;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format12bitRGB444;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB1555;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB4444;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format16bitBGR565;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format16bitRGB565;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format18BitBGR666;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format18bitARGB1665;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format18bitRGB666;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format19bitARGB1666;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format24BitABGR6666;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format24BitARGB6666;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format24bitARGB1887;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format24bitBGR888;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format24bitRGB888;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format25bitARGB1888;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format32bitABGR8888;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format32bitBGRA8888;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_Format8bitRGB332;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatCbYCrY;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatCrYCbY;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatL16;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatL2;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatL24;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatL32;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatL4;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatL8;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatMonochrome;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatRGBAFlexible;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatRGBFlexible;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer10bit;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer8bit;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer8bitcompressed;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYCbYCr;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYCrYCb;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411PackedPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411Planar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Flexible;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedSemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Planar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422SemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Flexible;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Interleaved;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar;

public class DeviceUtil {
    private static final String TYPE_AVC = "avc";
    private static final String TYPE_H264 = "h264";
    private static final String TYPE_ENCODER = "enc";
    private static final String TYPE_DECODER = "dec";

    private DeviceUtil() {}

    @NonNull
    public static String getDeviceInfo(@NonNull Context context) {
        return context.getString(R.string.brand, Build.BRAND)
            + context.getString(R.string.manufacturer, Build.MANUFACTURER)
            + context.getString(R.string.make, Build.DEVICE)
            + context.getString(R.string.board, Build.BOARD)
            + context.getString(R.string.product, Build.PRODUCT)
            + context.getString(R.string.api_level, Build.VERSION.SDK_INT);
    }

    @NonNull
    public static String getCaptureFormats(@NonNull Context context) {
        String captureFormats = getCameraInfo(context, CamcorderProfile.QUALITY_720P)
            + getCameraInfo(context, CamcorderProfile.QUALITY_1080P);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            captureFormats += getCameraInfo(context, CamcorderProfile.QUALITY_2160P);
        }

        return captureFormats;
    }

    @NonNull
    public static String getCodecList() {
        StringBuilder codecListStr = new StringBuilder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            for (MediaCodecInfo mediaCodecInfo: mediaCodecList.getCodecInfos()) {
                codecListStr.append(mediaCodecInfo.getName());
                codecListStr.append('\n');
            }
        }

        return codecListStr.toString();
    }

    @NonNull
    public static String getAvcDecoderCapabilities(@NonNull Context context) {
        StringBuilder codecListStr = new StringBuilder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            for (MediaCodecInfo mediaCodecInfo: mediaCodecList.getCodecInfos()) {
                String codecName = mediaCodecInfo.getName().toLowerCase(Locale.ROOT); {
                    if ((codecName.contains(TYPE_AVC) || codecName.contains(TYPE_H264)) && codecName.contains(TYPE_DECODER)) {
                        codecListStr.append(printCodecCapabilities(context, mediaCodecInfo));
                    }
                }
            }
        }

        return codecListStr.toString();
    }

    @NonNull
    public static String getAvcEncoderCapabilities(@NonNull Context context) {
        StringBuilder codecListStr = new StringBuilder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
                String codecName = mediaCodecInfo.getName().toLowerCase(Locale.ROOT);
                if ((codecName.contains(TYPE_AVC) || codecName.contains(TYPE_H264)) && codecName.contains(TYPE_ENCODER)) {
                    codecListStr.append(printCodecCapabilities(context, mediaCodecInfo));
                }
            }
        }

        return codecListStr.toString();
    }

    @NonNull
    private static String printCodecCapabilities(@NonNull Context context, @NonNull MediaCodecInfo mediaCodecInfo) {
        StringBuilder codecCapabilitiesStr = new StringBuilder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            codecCapabilitiesStr.append(mediaCodecInfo.getName());
            codecCapabilitiesStr.append('\n');

            String[] supportedTypes = mediaCodecInfo.getSupportedTypes();
            for (String supportedType : supportedTypes) {
                MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo.getCapabilitiesForType(supportedType);
                codecCapabilitiesStr.append(context.getString(R.string.supported_mime_type, codecCapabilities.getMimeType()));

                codecCapabilitiesStr.append(context.getString(R.string.color_formats));
                for (int colorFormat : codecCapabilities.colorFormats) {
                    codecCapabilitiesStr.append('\t');
                    codecCapabilitiesStr.append(getColorFormat(colorFormat));
                    codecCapabilitiesStr.append('\n');
                }

                if (mediaCodecInfo.isEncoder()) {
                    MediaCodecInfo.EncoderCapabilities encoderCapabilities = codecCapabilities.getEncoderCapabilities();
                    codecCapabilitiesStr.append(context.getString(R.string.encoder_complexity_range, encoderCapabilities.getComplexityRange()));
                }

                MediaCodecInfo.VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
                codecCapabilitiesStr.append(context.getString(R.string.bitrate_range, videoCapabilities.getBitrateRange()));
                codecCapabilitiesStr.append(context.getString(R.string.supported_heights, videoCapabilities.getSupportedHeights()));
                codecCapabilitiesStr.append(context.getString(R.string.supported_widths, videoCapabilities.getSupportedWidths()));
                codecCapabilitiesStr.append(context.getString(R.string.supported_frame_rates, videoCapabilities.getSupportedFrameRates()));

                MediaCodecInfo.CodecProfileLevel[] codecProfileLevels = codecCapabilities.profileLevels;
                codecCapabilitiesStr.append(context.getString(R.string.supported_profiles));
                for (MediaCodecInfo.CodecProfileLevel codecProfileLevel : codecProfileLevels) {
                    codecCapabilitiesStr.append('\t');
                    codecCapabilitiesStr.append(getCodecProfile(codecProfileLevel));
                    codecCapabilitiesStr.append(' ');
                    codecCapabilitiesStr.append(getCodecLevel(codecProfileLevel));
                    codecCapabilitiesStr.append('\n');
                }
            }
            codecCapabilitiesStr.append('\n');
        }

        return codecCapabilitiesStr.toString();
    }

    @NonNull
    private static String getCameraInfo(@NonNull Context context, int camcorderProfileId) {
        CamcorderProfile camcorderProfile = null;
        try {
            camcorderProfile = CamcorderProfile.get(camcorderProfileId);
        } catch (RuntimeException e) {
            // do nothing
        }

        if (camcorderProfile == null) {
            return context.getString(R.string.camcorder_profile_not_supported, camcorderProfileId) + '\n';
        }

        return context.getString(R.string.video_frame_width, camcorderProfile.videoFrameWidth)
            + context.getString(R.string.video_frame_height, camcorderProfile.videoFrameHeight)
            + context.getString(R.string.file_output_format, getFileFormat(camcorderProfile.fileFormat))
            + context.getString(R.string.video_codec, getVideoCodec(camcorderProfile.videoCodec))
            + context.getString(R.string.video_bitrate, camcorderProfile.videoBitRate / 1000000)
            + context.getString(R.string.video_frame_rate, camcorderProfile.videoFrameRate)
            + context.getString(R.string.audio_codec, getAudioCodec(camcorderProfile.audioCodec))
            + context.getString(R.string.audio_bitrate, camcorderProfile.audioBitRate / 1000)
            + context.getString(R.string.audio_sample_rate, camcorderProfile.audioSampleRate / 1000)
            + context.getString(R.string.audio_channels, camcorderProfile.audioChannels)
            + '\n';
    }

    @NonNull
    private static String getFileFormat(int formatId) {
        switch (formatId) {
            case 0:
                return "DEFAULT";
            case 1:
                return "THREE_GPP";
            case 2:
                return "MPEG_4";
            case 3:
                return "AMR_NB";
            case 4:
                return "AMR_WB";
            case 5:
                return "AAC_ADTS";
            case 9:
                return "WEBM";
            default:
                return String.valueOf(formatId);
        }
    }

    @NonNull
    private static String getVideoCodec(int codecId) {
        switch (codecId) {
            case 0:
                return "DEFAULT";
            case 1:
                return "H263";
            case 2:
                return "H264";
            case 3:
                return "MPEG_4_SP";
            case 4:
                return "VP8";
            case 5:
                return "HEVC";
            default:
                return String.valueOf(codecId);
        }
    }

    @NonNull
    private static String getAudioCodec(int codecId) {
        switch (codecId) {
            case 0:
                return "DEFAULT";
            case 1:
                return "AMR_NB";
            case 2:
                return "AMR_WB";
            case 3:
                return "AAC";
            case 4:
                return "HE_AAC";
            case 5:
                return "AAC_ELD";
            case 6:
                return "VORBIS";
            default:
                return String.valueOf(codecId);
        }
    }

    @NonNull
    private static String getColorFormat(int colorFormat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            switch (colorFormat) {
                case COLOR_Format32bitABGR8888:
                    return "COLOR_Format32bitABGR8888";
                case COLOR_FormatRGBAFlexible:
                    return "COLOR_FormatRGBAFlexible";
                case COLOR_FormatRGBFlexible:
                    return "COLOR_FormatRGBFlexible";
                case COLOR_FormatYUV420Flexible:
                    return "COLOR_FormatYUV420Flexible";
                case COLOR_FormatYUV422Flexible:
                    return "COLOR_FormatYUV422Flexible";
                case COLOR_FormatYUV444Flexible:
                    return "COLOR_FormatYUV444Flexible";
            }
        }
        switch (colorFormat) {
            case COLOR_Format12bitRGB444:
                return "COLOR_Format12bitRGB444";
            case COLOR_Format16bitARGB1555:
                return "COLOR_Format16bitARGB1555";
            case COLOR_Format16bitARGB4444:
                return "COLOR_Format16bitARGB4444";
            case COLOR_Format16bitBGR565:
                return "COLOR_Format16bitBGR565";
            case COLOR_Format16bitRGB565:
                return "COLOR_Format16bitRGB565";
            case COLOR_Format18BitBGR666:
                return "COLOR_Format18BitBGR666";
            case COLOR_Format18bitARGB1665:
                return "COLOR_Format18bitARGB1665";
            case COLOR_Format18bitRGB666:
                return "COLOR_Format18bitRGB666";
            case COLOR_Format19bitARGB1666:
                return "COLOR_Format19bitARGB1666";
            case COLOR_Format24BitABGR6666:
                return "COLOR_Format24BitABGR6666";
            case COLOR_Format24BitARGB6666:
                return "COLOR_Format24BitARGB6666";
            case COLOR_Format24bitARGB1887:
                return "COLOR_Format24bitARGB1887";
            case COLOR_Format24bitBGR888:
                return "COLOR_Format24bitBGR888";
            case COLOR_Format24bitRGB888:
                return "COLOR_Format24bitRGB888";
            case COLOR_Format25bitARGB1888:
                return "COLOR_Format25bitARGB1888";
            case COLOR_Format32bitARGB8888:
                return "COLOR_Format32bitARGB8888";
            case COLOR_Format32bitBGRA8888:
                return "COLOR_Format32bitBGRA8888";
            case COLOR_Format8bitRGB332:
                return "COLOR_Format8bitRGB332";
            case COLOR_FormatCbYCrY:
                return "COLOR_FormatCbYCrY";
            case COLOR_FormatCrYCbY:
                return "COLOR_FormatCrYCbY";
            case COLOR_FormatL16:
                return "COLOR_FormatL16";
            case COLOR_FormatL2:
                return "COLOR_FormatL2";
            case COLOR_FormatL24:
                return "COLOR_FormatL24";
            case COLOR_FormatL32:
                return "COLOR_FormatL32";
            case COLOR_FormatL4:
                return "COLOR_FormatL4";
            case COLOR_FormatL8:
                return "COLOR_FormatL8";
            case COLOR_FormatMonochrome:
                return "COLOR_FormatMonochrome";
            case COLOR_FormatRawBayer10bit:
                return "COLOR_FormatRawBayer10bit";
            case COLOR_FormatRawBayer8bit:
                return "COLOR_FormatRawBayer8bit";
            case COLOR_FormatRawBayer8bitcompressed:
                return "COLOR_FormatRawBayer8bitcompressed";
            case COLOR_FormatSurface:
                return "COLOR_FormatSurface";
            case COLOR_FormatYCbYCr:
                return "COLOR_FormatYCbYCr";
            case COLOR_FormatYCrYCb:
                return "COLOR_FormatYCrYCb";
            case COLOR_FormatYUV411PackedPlanar:
                return "COLOR_FormatYUV411PackedPlanar";
            case COLOR_FormatYUV411Planar:
                return "COLOR_FormatYUV411Planar";
            case COLOR_FormatYUV420PackedPlanar:
                return "COLOR_FormatYUV420PackedPlanar";
            case COLOR_FormatYUV420PackedSemiPlanar:
                return "COLOR_FormatYUV420PackedSemiPlanar";
            case COLOR_FormatYUV420Planar:
                return "COLOR_FormatYUV420Planar";
            case COLOR_FormatYUV420SemiPlanar:
                return "COLOR_FormatYUV420SemiPlanar";
            case COLOR_FormatYUV422PackedPlanar:
                return "COLOR_FormatYUV422PackedPlanar";
            case COLOR_FormatYUV422PackedSemiPlanar:
                return "COLOR_FormatYUV422PackedSemiPlanar";
            case COLOR_FormatYUV422Planar:
                return "COLOR_FormatYUV422Planar";
            case COLOR_FormatYUV422SemiPlanar:
                return "COLOR_FormatYUV422SemiPlanar";
            case COLOR_FormatYUV444Interleaved:
                return "COLOR_FormatYUV444Interleaved";
            case COLOR_QCOM_FormatYUV420SemiPlanar:
                return "COLOR_QCOM_FormatYUV420SemiPlanar";
            case COLOR_TI_FormatYUV420PackedSemiPlanar:
                return "COLOR_TI_FormatYUV420PackedSemiPlanar";
            default:
                return String.valueOf(colorFormat);
        }
    }

    @NonNull
    private static String getCodecProfile(@NonNull MediaCodecInfo.CodecProfileLevel codecProfileLevel) {
        switch (codecProfileLevel.profile) {
            case MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline:
                return "AVCProfileBaseline";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileMain:
                return "AVCProfileMain";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileExtended:
                return "AVCProfileExtended";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh:
                return "AVCProfileHigh";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10:
                return "AVCProfileHigh10";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422:
                return "AVCProfileHigh422";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444:
                return "AVCProfileHigh444";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline:
                return "AVCProfileConstrainedBaseline";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh:
                return "AVCProfileConstrainedHigh";
            default:
                return "Unknown";
        }
    }

    @NonNull
    private static String getCodecLevel(@NonNull MediaCodecInfo.CodecProfileLevel codecProfileLevel) {
        switch (codecProfileLevel.level) {
            case MediaCodecInfo.CodecProfileLevel.AVCLevel1:
                return "AVCLevel1";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel1b:
                return "AVCLevel1b";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel11:
                return "AVCLevel11";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel12:
                return "AVCLevel12";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel13:
                return "AVCLevel13";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel2:
                return "AVCLevel2";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel21:
                return "AVCLevel21";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel22:
                return "AVCLevel22";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel3:
                return "AVCLevel3";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel31:
                return "AVCLevel31";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel32:
                return "AVCLevel32";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel4:
                return "AVCLevel4";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel41:
                return "AVCLevel41";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel42:
                return "AVCLevel42";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel5:
                return "AVCLevel5";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel51:
                return "AVCLevel51";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel52:
                return "AVCLevel52";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel6:
                return "AVCLevel6";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel61:
                return "AVCLevel61";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel62:
                return "AVCLevel62";
            default:
                return "Unknown";
        }
    }
}
