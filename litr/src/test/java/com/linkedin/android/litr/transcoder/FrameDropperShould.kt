package com.linkedin.android.litr.transcoder

import android.media.MediaFormat
import com.linkedin.android.litr.codec.Decoder
import com.linkedin.android.litr.codec.Encoder
import com.linkedin.android.litr.io.MediaRange
import com.linkedin.android.litr.io.MediaSource
import com.linkedin.android.litr.io.MediaTarget
import com.linkedin.android.litr.render.GlVideoRenderer
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.MockitoAnnotations
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val VIDEO_TRACK = 0

class FrameDropperShould {

    @Mock
    lateinit var sourceFormat: MediaFormat

    @Mock
    lateinit var targetFormat: MediaFormat

    @Mock
    lateinit var mediaSource: MediaSource

    @Mock
    lateinit var mediaTarget: MediaTarget

    @Mock
    lateinit var encoder: Encoder

    @Mock
    lateinit var decoder: Decoder

    @Mock
    lateinit var renderer: GlVideoRenderer

    private var videoTrackTranscoder: VideoTrackTranscoder? = null

    @Before
    @Throws(Exception::class)
    fun setup() {
        MockitoAnnotations.openMocks(this)

        doReturn(sourceFormat).`when`(mediaSource).getTrackFormat(ArgumentMatchers.anyInt())
        `when`(mediaSource.selection).thenReturn(MediaRange(0, Long.MAX_VALUE))

        `when`(sourceFormat.containsKey(MediaFormat.KEY_FRAME_RATE)).thenReturn(true)
        `when`(targetFormat.containsKey(MediaFormat.KEY_FRAME_RATE)).thenReturn(true)
    }

    private fun initTranscoder() {
        videoTrackTranscoder = Mockito.spy(
            VideoTrackTranscoder(
                mediaSource,
                VIDEO_TRACK,
                mediaTarget,
                VIDEO_TRACK,
                targetFormat,
                renderer,
                decoder,
                encoder
            )
        ).also { it.start() }
    }

    @Test
    fun `not initialize frame dropper when source and target have the same frame rate`() {
        `when`(sourceFormat.getInteger(MediaFormat.KEY_FRAME_RATE)).thenReturn(60)
        `when`(targetFormat.getInteger(MediaFormat.KEY_FRAME_RATE)).thenReturn(60)
        initTranscoder()

        videoTrackTranscoder?.frameDropper.run {
            assertNull(this)
        }
    }

    @Test
    fun `not initialize frame dropper when the target has a higher frame rate than its source`() {
        `when`(sourceFormat.getInteger(MediaFormat.KEY_FRAME_RATE)).thenReturn(30)
        `when`(targetFormat.getInteger(MediaFormat.KEY_FRAME_RATE)).thenReturn(60)
        initTranscoder()

        videoTrackTranscoder?.frameDropper.run {
            assertNull(this)
        }
    }

    @Test
    fun `always render the first frame`() {
        `when`(sourceFormat.getInteger(MediaFormat.KEY_FRAME_RATE)).thenReturn(60)
        `when`(targetFormat.getInteger(MediaFormat.KEY_FRAME_RATE)).thenReturn(30)
        initTranscoder()

        videoTrackTranscoder?.frameDropper.run {
            assertNotNull(this)
            assertThat(shouldRender(), equalTo(true))
        }

    }

    @Test
    fun `render a quarter of the frames if the target's frame rate is four times less`() {
        val sourceFrameAmount = 200
        val sourceFrameRate = 60

        `when`(sourceFormat.getInteger(MediaFormat.KEY_FRAME_RATE)).thenReturn(sourceFrameRate)
        `when`(targetFormat.getInteger(MediaFormat.KEY_FRAME_RATE)).thenReturn(sourceFrameRate / 4)
        initTranscoder()

        videoTrackTranscoder?.frameDropper.run {
            assertNotNull(this)
            assertThat(
                (0 until sourceFrameAmount)
                    .map { shouldRender() }
                    .filter { it }
                    .size,
                equalTo(sourceFrameAmount / 4))
        }
    }
}
