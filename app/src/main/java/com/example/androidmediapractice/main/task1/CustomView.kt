package com.example.androidmediapractice.main.task1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import com.example.androidmediapractice.R


class CustomView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : View(context, attrs, defStyleAttr, defStyleRes) {
    constructor(context: Context) : this(context, null, 0, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)

    private val bitmap: Bitmap
    private val paint = Paint()

    companion object {
        const val DEFAULT_WIDTH = 120
        const val DEFAULT_HEIGHT = 120
    }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.CustomView, 0, 0)
                .apply {
                    try {
                        bitmap = getDrawable(R.styleable.CustomView_src)!!.toBitmap()
                    } finally {
                        recycle()
                    }
                }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            bitmap.width
        } else {
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        }

        val height = if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            bitmap.height
        } else {
            getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, ((this.width - bitmap.width) / 2).toFloat(), 0F, paint)
    }
}
