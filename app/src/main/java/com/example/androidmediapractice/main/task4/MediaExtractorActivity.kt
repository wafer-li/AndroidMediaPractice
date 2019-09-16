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
import java.io.File

@RuntimePermissions
class MediaExtractorActivity : AppCompatActivity() {

    private lateinit var mediaExtractor: MediaExtractor
    private lateinit var mediaFormat: MediaFormat
    private lateinit var mediaDecoder: MediaCodec

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
            initCodec(mediaFormat)
            play()
        }
    }

    private fun initMediaExtractor() {
        mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(File(getExternalFilesDir(null), "sample.mp4").absolutePath)
        val trackIndex = mediaExtractor.findTrackIndex {
            val mime = it.getString(MediaFormat.KEY_MIME)
            mime?.startsWith("video/") ?: false
        }
        mediaExtractor.selectTrack(trackIndex)
        mediaFormat = mediaExtractor.getTrackFormat(trackIndex)
    }


    private fun initCodec(mediaFormat: MediaFormat) {
        val mime = mediaFormat.getString(MediaFormat.KEY_MIME) ?: error("MIME Empty")
        mediaDecoder = MediaCodec.createDecoderByType(mime)
        mediaDecoder.configure(mediaFormat, mediaPlaySurfaceView.holder.surface, null, 0)
    }

    private fun play() {
        isPlaying = true
        mediaDecoder.start()
        while (true) {
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
                        mediaExtractor.sampleFlags.let { if (size <= 0) it or MediaCodec.BUFFER_FLAG_END_OF_STREAM else it }

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
        }
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