# Litr Muxers module

The Litr FFmpeg module provides integration with ffmpeg. Currently, it offers 
`NativeMediaMuxerMediaTarget`, which uses FFmpeg for muxing 
individual streams into a target file container.

## Build instructions (Linux, macOS)

It is necessary to manually build the FFmpeg library, so that gradle  can bundle the FFmpeg binaries
in the APK:

* Set the following shell variable:

```
cd "<path to project checkout>"
FFMPEG_MODULE_PATH="$(pwd)/litr-muxers/src/main"
```

* Download the [Android NDK][] and set its location in a shell variable.
  This build configuration has been tested on NDK r22b.

```
NDK_PATH="<path to Android NDK>"
```

* Set the host platform (use "darwin-x86_64" for Mac OS X):

```
HOST_PLATFORM="linux-x86_64"
```

* Fetch FFmpeg and checkout an appropriate branch. We cannot guarantee
  compatibility with all versions of FFmpeg. We currently recommend version 4.2:

```
cd "<preferred location for ffmpeg>" && \
git clone git://source.ffmpeg.org/ffmpeg && \
cd ffmpeg && \
git checkout release/4.2 && \
FFMPEG_PATH="$(pwd)"
```

*   Add a link to the FFmpeg source code in the FFmpeg module `cpp` directory.

```
cd "${FFMPEG_MODULE_PATH}/cpp" && \
ln -s "$FFMPEG_PATH" ffmpeg
```

* Execute `build_ffmpeg.sh` to build FFmpeg for `armeabi-v7a`, `arm64-v8a`,
  `x86` and `x86_64`. The script can be edited if you need to build for
  different architectures:

```
cd "${FFMPEG_MODULE_PATH}/cpp" && \
chmod +x build_ffmpeg.sh && \
./build_ffmpeg.sh \
  "${FFMPEG_MODULE_PATH}" "${NDK_PATH}" "${HOST_PLATFORM}"
```
