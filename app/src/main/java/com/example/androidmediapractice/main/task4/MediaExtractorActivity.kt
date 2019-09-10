package com.example.androidmediapractice.main.task4

import android.Manifest
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_media_extractor.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class MediaExtractorActivity : AppCompatActivity() {

    private lateinit var mediaExtractor: MediaExtractor
    private lateinit var mediaFormat: MediaFormat
    private lateinit var codec: MediaCodec

    private var width = 0
    private var height = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_extractor)

        mediaPlaySurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
            }

            override fun surfaceDestroyed(p0: SurfaceHolder?) {
            }

            override fun surfaceCreated(p0: SurfaceHolder?) {
                startPlayWithPermissionCheck()
            }
        })
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    internal fun startPlay() {
        initMediaExtractor()
        initVideoSpecs(mediaFormat)
        initCodec(mediaFormat)
    }

    private fun initMediaExtractor() {
        mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource("sample.mp4")
        val trackIndex = mediaExtractor.findTrackIndex {
            val mime = it.getString(MediaFormat.KEY_MIME)
            mime?.startsWith("video/") ?: false
        }
        mediaExtractor.selectTrack(trackIndex)
        mediaFormat = mediaExtractor.getTrackFormat(trackIndex)
    }

    private fun initVideoSpecs(mediaFormat: MediaFormat) {
        width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
        height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
    }

    private fun initCodec(mediaFormat: MediaFormat) {
        val mime = mediaFormat.getString(MediaFormat.KEY_MIME) ?: error("MIME Empty")
        codec = MediaCodec.createDecoderByType(mime)
        codec.configure(mediaFormat, mediaPlaySurfaceView.holder.surface, null, 0)
        codec.start()
    }

    private fun play() {
    }

    private fun MediaExtractor.findTrackIndex(block: (MediaFormat) -> Boolean): Int {
        for (i in 0 until trackCount) {
            val mediaFormat = this.getTrackFormat(i)
            if (block(mediaFormat)) {
                return i
            }
        }
        return -1
    }
}