package com.example.androidmediapractice.main.task8

import android.Manifest
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task8.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File

@RuntimePermissions
class Task8Activity : AppCompatActivity() {
    private lateinit var encoder: MediaCodec
    private lateinit var decoder: MediaCodec

    private var isEos = false

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task8)
        initEncoder()
        encodeH264Btn.setOnClickListener {
            encode()
        }
        decodeH264Btn.setOnClickListener {
            decode()
        }
    }

    private fun initEncoder() {
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val mediaFormat =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 1920 * 1080 * 5)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_CAPTURE_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
        encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun encode() {
        GlobalScope.launch(Dispatchers.IO) {
            encoder.start()
            val yuvFile = obtainYuvFile()
            val mp4File = obtainH264File()
            writeToEncoder(yuvFile)
            readFromEncoder(mp4File)
        }
    }

    private fun writeToEncoder(yuvFile: File) = GlobalScope.launch(Dispatchers.IO) {
        yuvFile.inputStream().channel.use {
            while (!isEos) {
                val inputBufferIndex = encoder.dequeueInputBuffer(0L)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                    val readCount = it.read(inputBuffer)
                    isEos = readCount <= 0
                    encoder.queueInputBuffer(
                        inputBufferIndex, 0, if (isEos) 0 else readCount,
                        0,
                        if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    )
                }
            }
        }
    }

    private fun readFromEncoder(outputFile: File) = GlobalScope.launch(Dispatchers.IO) {
        outputFile.outputStream().channel.use {
            while (true) {
                val bufferInfo = MediaCodec.BufferInfo()
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0L)
                if (bufferInfo.size <= 0 && isEos) {
                    encoder.stop()
                    encoder.release()
                    break
                } else if (outputBufferIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)?.apply {
                        position(bufferInfo.offset)
                        limit(bufferInfo.offset + bufferInfo.size)
                    }
                    it.write(outputBuffer)
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                }
            }
        }
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@Task8Activity, "Finish", Toast.LENGTH_SHORT).show()
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun decode() {
        GlobalScope.launch(Dispatchers.IO) {

        }
    }

    private fun obtainYuvFile() = File(getExternalFilesDir(null), "yuv420_888.yuv")

    private fun obtainH264File() = File(getExternalFilesDir(null), "output.h264")
}