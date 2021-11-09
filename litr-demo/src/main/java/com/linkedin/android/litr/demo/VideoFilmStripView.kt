package com.linkedin.android.litr.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.annotation.NonNull


class VideoFilmStripView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var bitmapList: MutableList<Bitmap?>? = null

    fun setFrameList(list: List<Bitmap?>) {
        bitmapList = list.toMutableList()
        invalidate()
    }

    override fun onDraw(@NonNull canvas: Canvas) {
        super.onDraw(canvas)
        var x = 0f
        bitmapList?.filterNotNull()?.forEach { bitmap ->
            canvas.drawBitmap(bitmap, x, 0f, null)
            x += bitmap.width
        }
    }

    fun setFrameAt(index: Int, bitmap: Bitmap?) {
        bitmapList?.set(index, bitmap)
        invalidate()
    }
}