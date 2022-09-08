# Change Log

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