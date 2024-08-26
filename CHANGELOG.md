# Change Log

## Version 1.5.7 (2024-08-26)

- Add 16KB page size support, required for Android 15 #277 by @vamshi-dhulipala

## Version 1.5.6 (2024-05-13)

- Release file descriptor even when releasing of android muxer fails #274 by @vamshi-dhulipala
- Refactors TransformationJob to handle terminal states in a more determisitic fashion #273 by @vamshi-dhulipala
- Fixes a null pointer crash (Github Issue #269) #271 by @vamshi-dhulipala
- Fixes a crash in demo app due to a null pointer #270 by @vamshi-dhulipala
- Fix potential buffer overflow issue #263 by @Nailik
- Always set TargetFormat value in TrackTransformationInfo object #262 by @vamshi-dhulipala
- Muxers: integrate native muxer with LiTr #253 by @IanDBird
- Upgrade Build Tools / SDK / Kotlin #249 by @IanDBird
- Muxers: Add native muxer implementation #248 by @IanDBird
- Add missing duration target track metadata to decoder output format #247
- Muxers: Clean up old artifacts if build fails #246 by @IanDBird
- Add missing duration target track metadata when adding track to muxer #244
- Muxers: Add native headers required for building (Alt) #243 by @IanDBird
- Implement skeleton of native litr-muxers module #239 by @IanDBird
- Move RecordCamera2Fragment to fragment package #234 by @IanDBird

## Version 1.5.5 (2023-01-25)

- Implement Camera2 support as a MediaSource [#232] (https://github.com/linkedin/LiTr/pull/232) by @IanDBird
- Add missing duration target track metadata when adding track to muxer [#244] (https://github.com/linkedin/LiTr/pull/244)
- Add missing duration target track metadata to decoder output format [#247] (https://github.com/linkedin/LiTr/pull/247)

## Version 1.5.4 (2022-12-23)

- Enforce transcoding of incompatible audio track(s) to compatible codecs [#220] (https://github.com/linkedin/LiTr/pull/220)
- Add TransformationOptions parameter to remove metadata tracks [#221] (https://github.com/linkedin/LiTr/pull/221)
- Fix for incorrect audio frame presentation time when trimming [#225] (https://github.com/linkedin/LiTr/pull/225)
- Default to 30 fps target frame rate if parameter is missing [#227] (https://github.com/linkedin/LiTr/pull/227)
- Set default buffer size when creating VideoRendererInputSurface [#228] (https://github.com/linkedin/LiTr/pull/228)
- Implement AudioRecord support for audio track [#229] (https://github.com/linkedin/LiTr/pull/229) by @IanDBird
- Allow transcoders advance to next track or EoS past selection end [#230] (https://github.com/linkedin/LiTr/pull/230)

## Version 1.5.3 (2022-09-08)

- Implementation of audio overlay filter [#199] (https://github.com/linkedin/LiTr/pull/199)
- Release BufferFilter's when renderer is released [#200] (https://github.com/linkedin/LiTr/pull/200)
- Allocate/deallocate native input/output buffers on init/release [#202] (https://github.com/linkedin/LiTr/pull/202)
- Add ability to reduce the target video's frame rate [#208] (https://github.com/linkedin/LiTr/pull/208) by @niekdev
- Add TransformationOptions flag to remove audio track(s) [#209] (https://github.com/linkedin/LiTr/pull/209)
- Fix for incorrect bitrate extraction in size estimation [#212] (https://github.com/linkedin/LiTr/pull/212)
- Use source (or default) frame rate when creating video MediaFormat [#213] (https://github.com/linkedin/LiTr/pull/213)
- Use null target MediaFormat for generic tracks [#214] (https://github.com/linkedin/LiTr/pull/214)

## Version 1.5.2 (2022-06-30)

- Audio filter that changes track volume [#193] (https://github.com/linkedin/LiTr/pull/193)
- Fix ConcurrentModificationException when removing jobs [#195] (https://github.com/linkedin/LiTr/pull/195) by @ReallyVasiliy
- Notify TransformationListener before releasing TransformationJob [#198] (https://github.com/linkedin/LiTr/pull/198) by @simekadam

## Version 1.5.1 (2022-05-10)

- Fix for incorrect orientation angle calculation during MVP matrix initialization [#185] (https://github.com/linkedin/LiTr/pull/185) by @kolesnikov-pasha
- AudioRenderer now picks correct AudioProcessor when audio format changes during transcoding [#190] (https://github.com/linkedin/LiTr/pull/190)

## Version 1.5.0 (2022-04-01)

- Fix for error callback is not called with InsufficientDiskSpace exception [#179] (https://github.com/linkedin/LiTr/pull/179) by @mikeshuttjuvo
- Add new transform method that accepts output URI [#182] (https://github.com/linkedin/LiTr/pull/182)
- Support for transcoding video to VP8/VP9 [#183] (https://github.com/linkedin/LiTr/pull/183)
- Remove deprecated MediaTransformer methods [#184] (https://github.com/linkedin/LiTr/pull/184)

## Version 1.4.19 (2022-03-07)

- Fix for missing documentation jar in release
- Fix for not yet started thumbnail jobs not being cleared when cancelled [#177](https://github.com/linkedin/LiTr/pull/177)

## Version 1.4.18 (2022-02-04)

This release introduces a lot of audio processing improvements and fixes:

- A new AudioRenderer, focused solely on audio processing
- Capability to change sampling rate with high performance and high quality, provided by integrating [Oboe resampler](https://github.com/google/oboe/tree/master/src/flowgraph/resampler).
- Capability to mix stereo audio to mono and vice versa.
- Support for audio filters.
- Support for writing raw audio into WAV file.
- Fix for dropped audio frames.

## Version 1.4.17 (2022-01-24)

- Allow non-negative reads from source buffer during transcoding [#133](https://github.com/linkedin/LiTr/pull/133)
- Fix GlVideoRenderer.hasFilters() logic [#135](https://github.com/linkedin/LiTr/pull/135)
- Obtain frame rate and interval from MediaFormat as either float or int [#138](https://github.com/linkedin/LiTr/pull/138)
- Do not set "profile" (MediaCodec.KEY_PROFILE) on the encoder [#139](https://github.com/linkedin/LiTr/pull/139)
- Add new target size estimation APIs to MediaTransformer [#155](https://github.com/linkedin/LiTr/pull/155)
- Fix for incorrect size estimation with range [#154](https://github.com/linkedin/LiTr/pull/154)
- New AudioRenderer implementation, with a render queue [#159](https://github.com/linkedin/LiTr/pull/159)
- Support for extracting a series of video thumbnails [#146](https://github.com/linkedin/LiTr/pull/146)

## Version 1.4.16 (2021-05-20)

- New MockVideoMediaSource and PassthroughDecoder implementations [#122](https://github.com/linkedin/LiTr/pull/122) which provide capability to create "empty" video

## Version 1.4.15 (2021-05-07)

- Remove fallback codec lookup configuration flags [#114](https://github.com/linkedin/LiTr/pull/115)
- Bump dependencies
- Bump build tools and compile/target SDK levels to 30
- Enable Kotlin

## Version 1.4.14 (2021-04-20)

- Support writing to Uri (by @Ma7moudHatem) [#114](https://github.com/linkedin/LiTr/pull/114)

## Version 1.4.13 (2021-04-02)

- Support for reducing audio sampling rate (by @Ma7moudHatem) [#111](https://github.com/linkedin/LiTr/pull/111)

## Version 1.4.12 (2021-03-28)

- Enhance the logic for finding a media codec for a media format (by @Ma7moudHatem) [#109](https://github.com/linkedin/LiTr/pull/109)

## Version 1.4.11 (2021-03-17)

- Release unused anymore MediaMetadataRetriever instance (by @Ma7moudHatem) [#106](https://github.com/linkedin/LiTr/pull/106)
- Make the default FRAME_WAIT_TIMEOUT equals to zero, because on some device the previous value cause a very very long processing time (by @Ma7moudHatem) [#105](https://github.com/linkedin/LiTr/pull/105)
- Fix a DivideByZero exception when track duration is shorter than 1 sec (by @Ma7moudHatem) [#104](https://github.com/linkedin/LiTr/pull/104)

## Version 1.4.10 (2021-03-14)

- Fix renderFrame logic in PassthroughSoftwareRenderer (by @Ma7moudHatem) [#103](https://github.com/linkedin/LiTr/pull/103)

## Version 1.4.9 (2021-02-26)

- Fix the fallbackToGetCodecByType logic (by @Ma7moudHatem) [#98](https://github.com/linkedin/LiTr/pull/98)

## Version 1.4.8 (2021-02-24)

- Moved artifact publishing from JCenter to Maven Central [#95](https://github.com/linkedin/LiTr/pull/95)