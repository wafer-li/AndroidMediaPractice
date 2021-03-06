package com.example.androidmediapractice.main.task3

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.view.SurfaceHolder
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import com.example.androidmediapractice.main.task3.YuvUtil.convertYuv420ToARGB
import kotlinx.android.synthetic.main.activity_preview_surface.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.nio.ByteBuffer


@RuntimePermissions
class PreviewSurfaceActivity : AppCompatActivity() {

    companion object {
        private const val PREVIEW_WIDTH = 1920
        private const val PREVIEW_HEIGHT = 1080
        private const val PREVIEW_SIZE = PREVIEW_HEIGHT * PREVIEW_WIDTH * 3 / 2
    }

    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_surface)
    }

    private fun initView() {
        previewSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
            }

            override fun surfaceDestroyed(p0: SurfaceHolder?) {
                isPlaying = false
            }

            override fun surfaceCreated(p0: SurfaceHolder?) {
                if (p0 != null) {
                    startPlay(p0)
                }
            }
        })
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    @WorkerThread
    internal fun startPlay(holder: SurfaceHolder) {
        GlobalScope.launch(Dispatchers.IO) {
            val file = File(getExternalFilesDir(null), "yuv420_888.yuv")
            isPlaying = true
            file.inputStream().buffered().use {
                val rotation = it.read()
                val buffer = ByteArray(PREVIEW_SIZE)
                while (isPlaying && it.read(buffer) > 0) {
                    val canvas = holder.lockCanvas()
                    canvas.drawColor(Color.BLACK)
                    val argb8888ByteArray =
                        convertYuv420ToARGB(buffer, PREVIEW_WIDTH, PREVIEW_HEIGHT)
                    val argb8888Buffer = ByteBuffer.wrap(argb8888ByteArray)
                    val bitmap =
                        Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(argb8888Buffer)
                    val matrix = Matrix()
                    matrix.postRotate(rotation.toFloat(), bitmap.width / 2F, bitmap.height / 2F)
                    val translate = (PREVIEW_WIDTH - PREVIEW_HEIGHT) / 2F
                    matrix.postTranslate(-translate, translate)
                    canvas.drawBitmap(bitmap, matrix, Paint())
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initView()
    }

    override fun onPause() {
        super.onPause()
        stopPlay()
    }

    private fun stopPlay() {
        isPlaying = false
    }
}