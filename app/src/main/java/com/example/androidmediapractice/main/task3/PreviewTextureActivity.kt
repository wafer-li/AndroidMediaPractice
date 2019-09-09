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
        GlobalScope.launch(Dispatchers.IO) {
            val file = File(getExternalFilesDir(null), "yuv420_888.yuv")
            isPlaying = true
            file.inputStream().buffered().use {
                val rotation = it.read()
                val buffer = ByteArray(PREVIEW_SIZE)
                while (isPlaying && it.read(buffer) > 0) {
                    val canvas = surface.lockCanvas(null)
                    canvas.drawColor(Color.BLACK)
                    val argb8888ByteArray =
                        YuvUtil.convertYuv420ToARGB(
                            buffer,
                            PREVIEW_WIDTH,
                            PREVIEW_HEIGHT
                        )
                    val argb8888Buffer = ByteBuffer.wrap(argb8888ByteArray)
                    val bitmap =
                        Bitmap.createBitmap(
                            PREVIEW_WIDTH,
                            PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888
                        )
                    bitmap.copyPixelsFromBuffer(argb8888Buffer)
                    val matrix = Matrix()
                    matrix.postRotate(rotation.toFloat(), bitmap.width / 2F, bitmap.height / 2F)
                    val translate = (PREVIEW_WIDTH - PREVIEW_HEIGHT) / 2F
                    matrix.postTranslate(-translate, translate)
                    canvas.drawBitmap(bitmap, matrix, Paint())
                    surface.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}