# LiTr
[![Build Status](https://img.shields.io/github/workflow/status/linkedin/LiTr/Merge%20checks)](https://img.shields.io/github/workflow/status/linkedin/LiTr/Merge%20checks)

LiTr (pronounced "lai-tr") is a lightweight video/audio transformation tool which supports transcoding video and audio tracks with optional frame modification.

In its current iteration LiTr supports:
- changing resolution and/or bitrate of a video track(s)
- changing sampling rate, channel count and/or bitrate of an audio track(s)
- overlaying bitmap watermark onto video track(s)
- applying different effects (brightness/contrast, saturation/hue, blur, etc.) to video pixels
- including/excluding tracks, which allows muxing/demuxing tracks
- transforming tracks individually (e.g. apply overlay to one video track, but not the other)
- positioning source video frame arbitrarily onto target video frame
- trimming video/audio
- creating "empty" video, or a video out of single image
- creating preview bitmap(s) (with filters applied) at specific timestamp(s) (filmstrip)
- writing raw audio into WAV container

By default, LiTr uses Android MediaCodec stack for hardware accelerated decoding/encoding and OpenGL for rendering. It also uses MediaExtractor and MediaMuxer to read/write media.

## Getting Started

Simply grab via Gradle:

```groovy
 implementation 'com.linkedin.android.litr:litr:1.5.3'
```
...or Maven:

```xml
<dependency>
  <groupId>com.linkedin.android.litr</groupId>
  <artifactId>litr</artifactId>
  <version>1.5.3</version>
</dependency>

```

## How to Transform a Video

First, instantiate `MediaTransformer` with a `Context` that can access `Uri`s you will be using for input and output. Most commonly, that will be an application context.

```java
MediaTransformer mediaTransformer = new MediaTransformer(getApplicationContext());
```

Then simply call `transform` method to transform your video:

```java
mediaTransformer.transform(requestId,
                           sourceVideoUri,
                           targetVideoFilePath,
                           targetVideoFormat,
                           targetAudioFormat,
                           videoTransformationListener,
                           transformationOptions);
```

Few notable things related to transformation:
- make sure to provide a unique `requestId`, it will be used when calling back on a listener, or needed when cancelling an ongoing transformation
- target formats will be applied to all tracks of that type, non video or audio tracks will be copied "as is"
- passing `null` target format means that you don't want to modify track(s) of that type
- transformation is performed asynchronously, listener will be called with any transformation progress or state changes
- by default listener callbacks happen on a UI thread, it is safe to update UI in listener implementation. It is also possible to have them on a non-UI transformation thread, for example, if any "heavy" works needs to be done in listener implementation.
- if you want to modify video frames, pass in a list of `GlFilter`s in `TransformationOptions`, which will be applied in order
- if you want to modify audio frames, pass in a list of `BufferFilter`s in `TransformationOptions`, which will be applied in order
- client can call `transform` multiple times, to queue transformation requests
- video will be written into MP4 container, we recommend using H.264 ("video/avc" MIME type) for target encoding. If VP8 or VP9 MIME type is used for target video track, audio track will be encoded using Opus codec, and tracks will be written into WebM container.
- progress update granularity is 100 by default, to match percentage, and can be set in `TransformationOptions`
- media can be optionally trimmed by specifying a `MediaRange` in `TransformationOptions`

Ongoing transformation can be cancelled by calling `cancel` with its `requestId`:

 ```java
mediaTransformer.cancel(requestId);
```

When you no longer need `MediaTransformer`, please release it. Note that `MediaTransformer` instance becomes unusable after you release it, you will have to instantiate a new one.

```java
mediaTransformer.release();
```

## Handling errors

When transformation fails, exception is not thrown, but rather provided in `TransformationListener.onError` callback. LiTr defines its own exceptions for different scenarios. For API >= 23, LiTr exception will also contain `MediaCodec.CodecException` as a cause.

## Reporting statistics

When possible, transformation statistics will be provided in listener callbacks. Statistics include source and target track formats, codecs used and transformation result and time for each track.

## Beyond Defaults

By default, LiTr uses Android MediaCodec stack to do all media work, and OpenGl for rendering. But this is not set in stone.

At high level, LiTr breaks down transformation into five essential steps:
- reading encoded frame from source container
- decoding source frame
- rendering a source frame onto target frame, optionally modifying it (for example, overlaying a bitmap)
- encoding target frame
- writing encoded target frame into target container

Each transformation step is performed by a component. Each component is abstracted as an interface:
- `MediaSource`
- `Decoder`
- `Renderer`
- `Encoder`
- `MediaTarget`

When using your own component implementations, make sure that output of a component matches the expected input of a next component. For example, if you are using a custom `Encoder` (AV1?), make sure it accepts whatever frame format `Renderer` produces (`GlSurface`, `ByteBuffer`) and outputs what `MediaTarget` expects as an input.

Custom components can be used in `TrackTransform`s in below "low level" transform method:

```java
transform(requestId,
         List<TrackTransform> trackTransforms,
         listener,
         granularity)
```

This API allows defining components and parameters per media track, thus allowing track based operations, such as muxing/demuxing tracks, transcoding different tracks differently, changing track order, etc.

## Using Filters

You can use custom filters to modify video/audio frames. If you are writing a custom video filter, implement `GlFilter` interface to make extra OpenGL draw operations. If you need to change how source video frame is rendered onto a target video frame, implement `GlFrameRender` interface. For audio filter, implement `BufferFilter`.

LiTr now has 40 new GPU accelerated video filters ported from [Mp4Composer-android](https://github.com/MasayukiSuda/Mp4Composer-android) and [android-gpuimage](https://github.com/cats-oss/android-gpuimage) projects. You can also create your own filter simply by configuring VideoFrameRenderFilter with your custom shader, with no extra coding!

All video/audio filters live in "filter pack" library, which is available via Gradle:

```groovy
 implementation 'com.linkedin.android.litr:litr-filters:1.5.3'
```
...or Maven:

```xml
<dependency>
    <groupId>com.linkedin.android.litr</groupId>
    <artifactId>litr-filters</artifactId>
    <version>1.5.3</version>
</dependency>

```

You can pass in a list of filters when transforming a video or audio track. Keep in mind that filters will be applied in the order they are in the list, so ordering matters.

## Using in Tests

`MediaTransformer` is very intentionally not a singleton, to allow easy mocking of it in unit tests. There is also `MockMediaTransformer` for UI tests, which can synchronously "play back" a sequence of listener callbacks.

## Testing

Core business logic in LiTr is well covered by unit tests. LiTr is designed to use dependency injection pattern, which makes it very easy to write JVM tests with mocked dependencies. We use Mockito framework for mocking.

## Demo App

LiTr comes with pretty useful demo app, which lets you transcode video/audio tracks with different parameters, in addition to providing sample code.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

For the versions available, see the [tags on this repository](https://github.com/linkedin/litr/tags).

## Snapshots

You can use snapshot builds to test the latest unreleased changes. A new snapshot is published
after every merge to the main branch by the [Deploy Snapshot Github Action workflow](.github/workflows/deploy-snapshot.yml).

Just add the Sonatype snapshot repository to your Gradle scripts:
```gradle
repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
    }
}
```

You can find the latest snapshot version to use in the [gradle.properties](gradle.properties) file.

## Authors

* **Izzat Bahadirov** - *Initial work* - [LiTr](https://github.com/linkedin/litr)

See also the list of [contributors](https://github.com/linkedin/litr/contributors) who participated in this project.

## License

This project is licensed under the BSD 2-Clause License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

* A huge thank you to [ypresto](https://github.com/ypresto/) for his pioneering work on [android-transcoder](https://github.com/ypresto/android-transcoder) project, which was an inspiration and heavy influence on LiTr
* A special thank you to [MasayukiSuda](https://github.com/MasayukiSuda) for his work on [Mp4Composer-android](https://github.com/MasayukiSuda/Mp4Composer-android) project, whose filters now power LiTr, and for his work on [ExoPlayerFilter](https://github.com/MasayukiSuda/ExoPlayerFilter) project which was a foundation for filter preview functionality in LiTr.
* A special thank you to [android-gpuimage](https://github.com/cats-oss/android-gpuimage) project for amazing filter collection, which have been ported into LiTr
* A thank you to Google's AOSP CTS team for writing Surface to Surface rendering implementation in OpenGL, which became a foundation for GlRenderer in LiTr
* A thank you to Google [Oboe](https://github.com/google/oboe) project for high quality audio resampling implementation, which became a foundation of audio processing in LiTr
* A shout out to my awesome colleagues Amita Sahasrabudhe, Long Peng, Keerthi Korrapati and Vasiliy Kulakov for contributions and code reviews
* A shout out to my colleague Vidhya Pandurangan for prototyping video trimming, which now became a feature
* A shout out to our designer Mauroof Ahmed for giving LiTr a visual identity
* A shout out to [PurpleBooth](https://gist.github.com/PurpleBooth/) for very useful [README.md template](https://gist.github.com/PurpleBooth/109311bb0361f32d87a2)
* A shout out to [Ma7moudHatem](https://github.com/Ma7moudHatem) for his invaluable contributions to LiTr
