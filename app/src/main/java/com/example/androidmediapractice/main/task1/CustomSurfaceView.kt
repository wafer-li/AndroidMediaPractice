package com.example.androidmediapractice.main.task1

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.drawable.toBitmap
import com.example.androidmediapractice.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class CustomSurfaceView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?) : this(context, null)

    private var widthMeasureSpec: Int = 0
    private var heightMeasureSpec: Int = 0

    init {
        holder.addCallback(this)
        keepScreenOn = true
        isFocusableInTouchMode = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (this.widthMeasureSpec == 0)
            this.widthMeasureSpec = widthMeasureSpec
        if (this.heightMeasureSpec == 0)
            this.heightMeasureSpec = heightMeasureSpec
        super.onMeasure(this.widthMeasureSpec, this.heightMeasureSpec)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        thread(start = true) {
            if (holder != null) {
                draw(holder)
            }
        }
    }

    private fun draw(holder: SurfaceHolder) {
        val bitmap = context.getDrawable(R.mipmap.ic_launcher)?.toBitmap()
        if (bitmap != null) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(bitmap.height, MeasureSpec.AT_MOST)
            GlobalScope.launch(Dispatchers.Main) {
                requestLayout()
            }
            val canvas = holder.lockCanvas(Rect(0, 0, this.width, this.height))
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bitmap, ((this.width - bitmap.width) / 2).toFloat(), 0F, Paint())
            holder.unlockCanvasAndPost(canvas)
        }
    }
}
