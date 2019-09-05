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
import kotlinx.android.synthetic.main.activity_preview_surface.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min


@RuntimePermissions
class PreviewSurfaceActivity : AppCompatActivity() {

    companion object {
        private const val PREVIEW_WIDTH = 1920
        private const val PREVIEW_HEIGHT = 1080
        private const val PREVIEW_SIZE = PREVIEW_HEIGHT * PREVIEW_WIDTH * 3 / 2
        private val Y1 = LongArray(256)
        private val Y2 = LongArray(256)
        private val U = LongArray(256)
        private val V = LongArray(256)
    }

    private var isPlaying = false
    private var isInitialized = false

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
                    val rotatedBitmap =
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    canvas.drawBitmap(rotatedBitmap, 0F, 0F, Paint())
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    private fun initTable() {
        var i = 0
        if (isInitialized) {
            return
        }
        // Initialize table
        while (i < 256) {
            V[i] = (15938 * i - 2221300).toLong()
            U[i] = (20238 * i - 2771300).toLong()
            Y1[i] = (11644 * i).toLong()
            Y2[i] = (19837 * i - 311710).toLong()
            i++
        }
        isInitialized = true
    }

    private fun convertYuv420ToARGB(yuv420Bytes: ByteArray, width: Int, height: Int): ByteArray {
        // upscaling to yuv444
        val yuv444Bytes = yuv420ToYuv444(yuv420Bytes, width, height)
        val argb8888Bytes = ByteArray(yuv444Bytes.size + (width * height))

        initTable()

        for (i in 0 until width * height) {
            val py = i
            val pu = i + width * height
            val pv = i + 2 * width * height

            val y = yuv444Bytes[py].toInt() and 0xff
            val u = yuv444Bytes[pu].toInt() and 0xff
            val v = yuv444Bytes[pv].toInt() and 0xff

            val r = max(
                0,
                min(
                    255,
                    (V[v] + Y1[y]) / 10000
                )
            )
            val b = max(
                0,
                min(
                    255,
                    (U[u] + Y1[y]) / 10000
                )
            )
            val g =
                max(0, min(255, (Y2[y] - 5094 * r - 1942 * b) / 10000))

            argb8888Bytes[4 * i] = r.toByte()
            argb8888Bytes[4 * i + 1] = g.toByte()
            argb8888Bytes[4 * i + 2] = b.toByte()
            argb8888Bytes[4 * i + 3] = 255.toByte()
        }
        return argb8888Bytes
    }

    private fun yuv420ToYuv444(yuv420Bytes: ByteArray, width: Int, height: Int): ByteArray {
        val size = width * height
        val yuv444Bytes = ByteArray(width * height * 3)

        for (y in 0 until size) {
            yuv444Bytes[y] = yuv420Bytes[y]
        }

        val originUIndex = size
        val originVIndex = originUIndex + width * height / 4
        val desUIndex = size
        val desVIndex = desUIndex + size
        for (h in 0 until height step 2) {
            for (w in 0 until width step 2) {
                val originU = yuv420Bytes[originUIndex + h / 2 * width / 2 + w / 2]
                yuv444Bytes[desUIndex + h * width + w] = originU
                yuv444Bytes[desUIndex + h * width + w + 1] = originU
                yuv444Bytes[desUIndex + (h + 1) * width + w] = originU
                yuv444Bytes[desUIndex + (h + 1) * width + w + 1] = originU

                val originV = yuv420Bytes[originVIndex + h / 2 * width / 2 + w / 2]
                yuv444Bytes[desVIndex + h * width + w] = originV
                yuv444Bytes[desVIndex + h * width + w + 1] = originV
                yuv444Bytes[desVIndex + (h + 1) * width + w] = originV
                yuv444Bytes[desVIndex + (h + 1) * width + w + 1] = originV
            }
        }

        return yuv444Bytes
    }
}