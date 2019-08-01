package com.example.androidmediapractice.main.task1

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.concurrent.thread

class CustomSurfaceView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?) : this(context, null)

    private var isDrawing = false

    init {
        holder.addCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        isDrawing = true
        thread(start = true) {
        }
    }
}
