package com.linkedin.android.litr.render

/**
 * A simple video frame dropper which provides a [shouldRender] function. May be used when the
 * target video should have a lower frame rate than its source video.
 *
 * @see <a href="https://github.com/linkedin/LiTr/issues/120#issuecomment-859318538">Suggested by @alexvasilkov</a>
 */
internal interface FrameDropper {

    fun shouldRender(): Boolean
}

class DefaultFrameDropper(
    inputFps: Int,
    outputFps: Int,
) : FrameDropper {

    private val inputSpf = 1.0 / inputFps
    private val outputSpf = 1.0 / outputFps
    private var currentSpf = 0.0
    private var frameCount = 0

    override fun shouldRender(): Boolean {
        currentSpf += inputSpf

        return when {
            frameCount++ == 0 -> true
            currentSpf > outputSpf -> true.also { currentSpf -= outputSpf }
            else -> false
        }
    }
}
