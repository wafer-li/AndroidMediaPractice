package com.example.androidmediapractice.main.task7

import android.Manifest
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Bundle
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task7.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.nio.channels.FileChannel

@RuntimePermissions
class Task7Activity : AppCompatActivity() {
    private lateinit var encoder: MediaCodec
    private lateinit var decoder: MediaCodec

    companion object {
        private const val BYTES_PER_SECOND = 44100 * 1 * 2
    }

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
        setContentView(R.layout.activity_task7)
        prepareSamplePcmWithPermissionCheck()
        initEncoder()
        initOnClickListener()
    }

    private fun initOnClickListener() {
        encodeAacBtn.setOnClickListener {
            encode()
        }
    }

    private fun initEncoder() {
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaFormat =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 96000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
            }
        encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    private fun encode() = GlobalScope.launch(Dispatchers.IO) {
        encoder.start()
        val inputChannel =
            File(getExternalFilesDir(null), "task7/sample.pcm").outputStream().channel
        val outputChannel =
            File(getExternalFilesDir(null), "task7/output.aac").outputStream().channel
        writeToMediaCodec(inputChannel)
        readFromMediaCodec(outputChannel)
    }

    @WorkerThread
    private fun writeToMediaCodec(channel: FileChannel) = GlobalScope.launch(Dispatchers.IO) {
        channel.use { fileChannel: FileChannel ->
            do {
                val inputBufferIndex = encoder.dequeueInputBuffer(0L)
                val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                val readCount = fileChannel.read(inputBuffer)
                val isEos = readCount < 0
                encoder.queueInputBuffer(
                    inputBufferIndex, 0,
                    if (isEos) 0 else readCount,
                    0,
                    if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else MediaCodec.BUFFER_FLAG_KEY_FRAME
                )
            } while (!isEos)
        }
    }

    @WorkerThread
    private fun readFromMediaCodec(channel: FileChannel) =
        GlobalScope.launch(Dispatchers.IO) {
            channel.use { fileChanel ->
                while (true) {
                    val bufferInfo = MediaCodec.BufferInfo()
                    val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0L)

                    if (bufferInfo.size <= 0) {
                        encoder.stop()
                        encoder.release()
                        break
                    } else if (outputBufferIndex >= 0) {
                        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                        fileChanel.write(outputBuffer)
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
            }
        }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun prepareSamplePcm() {
        GlobalScope.launch(Dispatchers.IO) {
            val file = File(getExternalFilesDir(null), "task7").let {
                it.mkdir()
                File(it, "sample.pcm")
            }
            val isPrepared = file.exists()
            if (!isPrepared) {
                val outputStream = file.outputStream()
                resources.assets.open("task7/sample.pcm").use {
                    while (it.available() > 0) {
                        outputStream.write(it.read())
                    }
                }
            }
            GlobalScope.launch(Dispatchers.Main) {
                encodeAacBtn.isEnabled = true
                decodeAacBtn.isEnabled = true
            }
        }
    }
}