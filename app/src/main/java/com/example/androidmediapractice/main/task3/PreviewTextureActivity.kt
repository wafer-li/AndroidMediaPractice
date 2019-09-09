package com.example.androidmediapractice.main.task3

import android.Manifest
import android.graphics.*
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_preview_texture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.nio.ByteBuffer

@RuntimePermissions
class PreviewTextureActivity : AppCompatActivity() {
    companion object {
        private const val PREVIEW_WIDTH = 1920
        private const val PREVIEW_HEIGHT = 1080
        private const val PREVIEW_SIZE = PREVIEW_HEIGHT * PREVIEW_WIDTH * 3 / 2
    }

    private var isPlaying = false

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
            isPlaying = false
            return true
        }

        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
            startPlayWithPermissionCheck(p0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_texture)
        initView()
    }

    private fun initView() {
        previewTextureView.surfaceTextureListener = this.surfaceTextureListener
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    internal fun startPlay(texture: SurfaceTexture?) {
        if (texture == null) return
        val surface = Surface(texture)
        isPlaying = true
        GlobalScope.launch(Dispatchers.IO) {
            File(getExternalFilesDir(null), "yuv420_888.yuv").inputStream().buffered().use {
                val rotation = it.read()
                val yuv420Bytes = ByteArray(PREVIEW_SIZE)
                val isExchangeWidthAndHeight = rotation == 90 || rotation == 270
                val rect = Rect(
                    0, 0,
                    if (isExchangeWidthAndHeight) PREVIEW_HEIGHT else PREVIEW_WIDTH,
                    if (isExchangeWidthAndHeight) PREVIEW_WIDTH else PREVIEW_HEIGHT
                )
                while (isPlaying && it.read(yuv420Bytes) > 0) {
                    val canvas = surface.lockCanvas(rect)
                    val argbBytes =
                        YuvUtil.convertYuv420ToARGB(yuv420Bytes, PREVIEW_WIDTH, PREVIEW_HEIGHT)
                    val bitmap =
                        Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argbBytes))
                    val matrix = Matrix().apply {
                        postRotate(rotation.toFloat())
                        val translate = (PREVIEW_WIDTH - PREVIEW_HEIGHT) / 2F
                        postTranslate(-translate, translate)
                    }
                    canvas.drawBitmap(bitmap, matrix, Paint())
                    surface.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}