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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class MediaExtractorActivity : AppCompatActivity() {

    private lateinit var mediaExtractor: MediaExtractor
    private lateinit var mediaFormat: MediaFormat
    private lateinit var mediaDecoder: MediaCodec

    private var width = 0
    private var height = 0

    private var isPlaying = false

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

    override fun onPause() {
        super.onPause()
        stopPlay()
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    internal fun startPlay() {
        GlobalScope.launch(Dispatchers.IO) {
            initMediaExtractor()
            initVideoSpecs(mediaFormat)
            initCodec(mediaFormat)
            play()
        }
    }

    private fun initMediaExtractor() {
        mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(resources.assets.openFd("sample.mp4"))
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
        mediaDecoder = MediaCodec.createDecoderByType(mime)
        mediaDecoder.configure(mediaFormat, mediaPlaySurfaceView.holder.surface, null, 0)
    }

    private fun play() {
        isPlaying = true
        mediaDecoder.start()
        do {
            val isEos =
                mediaExtractor.sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == MediaCodec.BUFFER_FLAG_END_OF_STREAM
            if (!isEos) {
                val inputBufferIndex = mediaDecoder.dequeueInputBuffer(0L)
                if (inputBufferIndex >= 0) {
                    val buffer = mediaDecoder.getInputBuffer(inputBufferIndex)
                    val size = if (buffer != null) {
                        mediaExtractor.readSampleData(buffer, 0)
                    } else -1
                    val flag =
                        mediaExtractor.sampleFlags.let { if (size <= 0) it and MediaCodec.BUFFER_FLAG_END_OF_STREAM else it }

                    mediaDecoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        size,
                        mediaExtractor.sampleTime,
                        flag
                    )

                    if (buffer != null) {
                        mediaExtractor.advance()
                    }
                }
            }

            val outputInfo = MediaCodec.BufferInfo()
            val outputBufferIndex = mediaDecoder.dequeueOutputBuffer(outputInfo, 0L)

            if (outputInfo.size <= 0 && isEos) {
                stopPlay()
                break
            } else if (outputBufferIndex >= 0) {
                mediaDecoder.releaseOutputBuffer(outputBufferIndex, true)
            }

        } while (true)
    }

    private fun stopPlay() {
        if (isPlaying) {
            isPlaying = false
            mediaDecoder.stop()
            mediaDecoder.release()
            mediaExtractor.release()
        }
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