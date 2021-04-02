/*
 * Copyright (C) 2021 natario1 Transcoder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// from: https://github.com/natario1/Transcoder/blob/main/lib/src/main/java/com/otaliastudios/transcoder/resample/PassThroughAudioResampler.java
// modified: changed the signature of resample() method
// modified: removed the first if-condition in resample() method
package com.linkedin.android.litr.resample;

import android.media.MediaFormat;
import androidx.annotation.NonNull;
import java.nio.ByteBuffer;

/**
 * An {@link AudioResampler} that does nothing, meant to be used when sample
 * rates are identical.
 */
public class PassThroughAudioResampler implements AudioResampler {

    @Override
    public void resample(@NonNull ByteBuffer inputBuffer, @NonNull ByteBuffer outputBuffer,
            @NonNull MediaFormat sourceMediaFormat, @NonNull MediaFormat targetMediaFormat) {
        outputBuffer.put(inputBuffer);
    }
}
