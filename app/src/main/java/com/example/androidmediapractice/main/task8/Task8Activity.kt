package com.example.androidmediapractice.main.task8

import android.Manifest
import android.media.Image
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
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.experimental.and

@RuntimePermissions
class Task8Activity : AppCompatActivity() {
    private lateinit var encoder: MediaCodec
    private lateinit var decoder: MediaCodec

    private var outputFormat = MediaFormat()

    private var isEos = false
    private var frameIndex = 0

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
        encodeH264Btn.setOnClickListener {
            initEncoder()
            encode()
        }
        decodeH264Btn.setOnClickListener {
            initDecoder()
            decode()
        }
    }

    @Suppress("DEPRECATION")
    private fun initEncoder() {
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val mediaFormat =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    // Our YUV file is stored as YUV420Planar
                    // MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible is for decoder
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 1920 * 1080 * 5)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(
                    MediaFormat.KEY_BITRATE_MODE,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
                )
                setInteger(MediaFormat.KEY_CAPTURE_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
        encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    private fun initDecoder() {
        decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val mediaFormat =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080).apply {
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            }
        decoder.configure(mediaFormat, null, null, 0)
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun encode() {
        GlobalScope.launch(Dispatchers.IO) {
            isEos = false
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
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    outputFormat = encoder.outputFormat
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
            isEos = false
            decoder.start()
            val outputFile = obtainOutPutYuvFile()
            val inputH264File = obtainH264File()
            writeToDecoder(inputH264File)
            readFromDecoder(outputFile)
        }
    }

    private fun writeToDecoder(file: File) = GlobalScope.launch(Dispatchers.IO) {
        RandomAccessFile(file, "r").use {
            while (!isEos) {
                val inputBufferIndex = decoder.dequeueInputBuffer(0L)
                if (inputBufferIndex >= 0) {
                    val inputBuffer =
                        decoder.getInputBuffer(inputBufferIndex) ?: error("InputBufferIndex Error")
                    val naluBytes = obtainNALU(it, inputBuffer.capacity())
                    val readCount = naluBytes.size
                    inputBuffer.put(naluBytes)
                    isEos = readCount <= 0
                    decoder.queueInputBuffer(
                        inputBufferIndex, 0,
                        if (isEos) 0 else readCount, 0,
                        if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        else if (isSpsNALU(naluBytes) || isPpsNALU(naluBytes)) MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                        else if (isIdrNALU(naluBytes)) MediaCodec.BUFFER_FLAG_KEY_FRAME
                        else 0
                    )
                }
            }
        }
    }

    private fun readFromDecoder(outputFile: File) = GlobalScope.launch(Dispatchers.IO) {
        outputFile.outputStream().channel.use {
            while (true) {
                val bufferInfo = MediaCodec.BufferInfo()
                val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0L)
                if (bufferInfo.size <= 0 && bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    decoder.stop()
                    decoder.release()
                    break
                } else if (outputBufferIndex >= 0) {
                    val image = decoder.getOutputImage(outputBufferIndex)
                        ?: error("outputBufferIndex Error")
                    val cropRect = image.cropRect
                    image.planes[0].buffer.limit(cropRect.width() * cropRect.height())
                    image.planes[1].buffer.limit((cropRect.width() * cropRect.height() / 2))
                    image.planes[2].buffer.limit(cropRect.width() * cropRect.height() / 2)
                    it.write(ByteBuffer.wrap(obtainImageBytes(1920, 1080, image.planes)))
                    image.close()
                    decoder.releaseOutputBuffer(outputBufferIndex, false)
                }
            }
        }
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@Task8Activity, "Finish", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isSpsNALU(bytes: ByteArray): Boolean {
        return (isStartCode4(bytes, 0) && ((bytes[4] and (0x1f).toByte()) == 7.toByte()))
                || (isStartCode3(bytes, 0) && ((bytes[3] and (0x1f).toByte()) == 7.toByte()))
    }

    private fun isPpsNALU(bytes: ByteArray): Boolean {
        return (isStartCode4(bytes, 0) && ((bytes[4] and (0x1f).toByte()) == 8.toByte()))
                || (isStartCode3(bytes, 0) && ((bytes[3] and (0x1f).toByte()) == 8.toByte()))
    }

    private fun isIdrNALU(bytes: ByteArray): Boolean {
        return (isStartCode4(bytes, 0) && ((bytes[4] and (0x1f).toByte()) == 5.toByte()))
                || (isStartCode3(bytes, 0) && ((bytes[3] and (0x1f).toByte()) == 5.toByte()))
    }

    private fun obtainNALU(rf: RandomAccessFile, maxSize: Int): ByteArray {
        val bytes = ByteArray(maxSize + 5)
        var currentPosition = -1
        val readCount = rf.read(bytes, 0, 4)
        if (isStartCode4(bytes, 0)) {
            currentPosition = 4
        } else {
            rf.seek(rf.filePointer - 4)
            rf.read(bytes, 0, 3)
            if (isStartCode3(bytes, 0)) {
                currentPosition = 3
            }
        }

        var isFindNextStartCode = readCount <= 0
        var nextStartPosition = currentPosition
        while (!isFindNextStartCode) {
            val hex = rf.read()
            if (hex == -1) {
                nextStartPosition = currentPosition
                break
            } else {
                bytes[currentPosition++] = hex.toByte()
            }
            if (isStartCode4(bytes, currentPosition - 4)) {
                nextStartPosition = currentPosition - 4
                rf.seek(rf.filePointer - 4)
                isFindNextStartCode = true
            } else if (isStartCode3(bytes, currentPosition - 3)) {
                nextStartPosition = currentPosition - 3
                rf.seek(rf.filePointer - 3)
                isFindNextStartCode = true
            }
        }
        return nextStartPosition.run {
            if (nextStartPosition <= 0)
                ByteArray(0)
            else bytes.copyOfRange(
                0,
                nextStartPosition
            )
        }
    }

    private fun isStartCode4(bytes: ByteArray, offset: Int): Boolean {
        return bytes[offset + 0].toInt() == 0 &&
                bytes[offset + 1].toInt() == 0 &&
                bytes[offset + 2].toInt() == 0 &&
                bytes[offset + 3].toInt() == 1
    }

    private fun isStartCode3(bytes: ByteArray, offset: Int): Boolean {
        return bytes[offset + 0].toInt() == 0 &&
                bytes[offset + 1].toInt() == 0 &&
                bytes[offset + 2].toInt() == 1
    }

    private fun obtainImageBytes(width: Int, height: Int, planes: Array<Image.Plane>): ByteArray {
        val bytes = ByteArray(width * height * 3 / 2)
        var count = 0
        planes.forEach {
            val buffer = it.buffer
            val pixelStride = it.pixelStride
            for (i in 0 until buffer.remaining() step pixelStride) {
                bytes[count++] = buffer.get(i)
            }
        }
        return bytes
    }

    private fun obtainYuvFile() = File(getExternalFilesDir(null), "yuv420_888.yuv")

    private fun obtainH264File() = File(getExternalFilesDir(null), "output.h264")

    private fun obtainOutPutYuvFile() = File(getExternalFilesDir(null), "output.yuv")


}