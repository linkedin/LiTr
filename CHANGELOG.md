# Change Log

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