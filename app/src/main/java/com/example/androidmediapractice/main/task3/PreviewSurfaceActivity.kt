package com.example.androidmediapractice.main.task3

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.SurfaceHolder
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_preview_surface.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt


@RuntimePermissions
class PreviewSurfaceActivity : AppCompatActivity() {

    companion object {
        private const val PREVIEW_WIDTH = 1080
        private const val PREVIEW_HEIGHT = 1920
        private const val PREVIEW_SIZE = PREVIEW_HEIGHT * PREVIEW_WIDTH * 8 * 1.5
    }

    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_surface)
        initView()
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
            val file = File(getExternalFilesDir(null), "yuv420_888.dat")
            isPlaying = true
            file.inputStream().buffered().use {
                val buffer = ByteArray(PREVIEW_SIZE.roundToInt())
                while (isPlaying && it.read(buffer) > 0) {
                    val canvas = holder.lockCanvas()
                    canvas.drawColor(Color.BLACK)
                    val argb8888ByteArray =
                        convertYuv420ToARGB(buffer, PREVIEW_WIDTH, PREVIEW_HEIGHT)
                    val argb8888Buffer = ByteBuffer.wrap(argb8888ByteArray)
                    val bitmap =
                        Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(argb8888Buffer)
                    canvas.drawBitmap(bitmap, 0F, 0F, Paint())
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }


    private fun convertYuv420ToARGB(yuv420Bytes: ByteArray, width: Int, height: Int): ByteArray {
        // upscaling to yuv444
        val yuv444Bytes = yuv420ToYuv444(yuv420Bytes, width, height)
        val argb8888Bytes = ByteArray(yuv444Bytes.size + (width * height))

        /**
         * Transformation Matrix : BT.709
         * R = 1.0  0.0         1.28033
         * G = 1.0  -0.21482    -0.38509
         * B = 1.0  2.12798     0.0
         */

        for (w in 0 until width) {
            for (h in 0 until height) {
                val yIndex = w + h
                val uIndex = yIndex + width * height
                val vIndex = uIndex + width * height
                val aIndex = vIndex + width * height

                val y = yuv444Bytes[yIndex]
                val u = yuv444Bytes[uIndex]
                val v = yuv444Bytes[vIndex]

                val r = y + 1.28033 * v
                val g = y - 0.21482 * u - 0.38509 * v
                val b = y + 2.12798 * u
                val a = 0xFF

                argb8888Bytes[yIndex] = r.roundToInt().toByte()
                argb8888Bytes[uIndex] = g.roundToInt().toByte()
                argb8888Bytes[vIndex] = b.roundToInt().toByte()
                argb8888Bytes[aIndex] = a.toByte()
            }
        }
        return argb8888Bytes
    }

    private fun yuv420ToYuv444(yuv420Bytes: ByteArray, width: Int, height: Int): ByteArray {
        val size = width * height
        val yuv444Bytes = ByteArray(width * height * 3)
        val vStart = size + width * height / 4

        for (y in 0 until size) {
            yuv444Bytes[y] = yuv420Bytes[y]
        }

        // Upscaling U
        for (u in size until vStart) {
            val currentU = yuv420Bytes[u]
            yuv444Bytes[u] = currentU
            yuv444Bytes[u + 1] = currentU
            yuv444Bytes[u + width / 2] = currentU
            yuv444Bytes[(u + width / 2) + 1] = currentU
        }

        // Upscaling V
        for (v in vStart until (width * height * 1.5).roundToInt()) {
            val currentU = yuv420Bytes[v]
            yuv444Bytes[v] = currentU
            yuv444Bytes[v + 1] = currentU
            yuv444Bytes[v + width / 2] = currentU
            yuv444Bytes[(v + width / 2) + 1] = currentU
        }

        return yuv444Bytes
    }
}