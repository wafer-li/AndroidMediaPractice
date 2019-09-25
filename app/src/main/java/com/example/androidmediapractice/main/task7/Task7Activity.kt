package com.example.androidmediapractice.main.task7

import android.Manifest
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.widget.Toast
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
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


@RuntimePermissions
class Task7Activity : AppCompatActivity() {
    private lateinit var encoder: MediaCodec
    private lateinit var decoder: MediaCodec

    private var isEos = false

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
        initDecoder()
        initOnClickListener()
    }

    private fun initOnClickListener() {
        encodeAacBtn.setOnClickListener {
            encode()
        }
        decodeAacBtn.setOnClickListener {
            decode()
        }
    }

    private fun initEncoder() {
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaFormat =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 96000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC
                )
            }
        encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    private fun initDecoder() {
        decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaFormat =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 96000)
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC
                )
                setInteger(MediaFormat.KEY_IS_ADTS, 0)
                fun createCodecSpecificData(
                    audioObjectType: Int,
                    sampleFrequencyIndex: Int,
                    channelConfig: Int
                ): ByteBuffer {
                    // AAC CSD_0 Format
                    /*
                     * oooo offf fccc c000
                     * o - audio object type
                     * f - sample frequency
                     * c - channel configuration
                     */
                    val csd0 = ByteArray(2)
                    csd0[0] = ((audioObjectType shl 3) or (sampleFrequencyIndex ushr 1)).toByte()
                    csd0[1] =
                        (((sampleFrequencyIndex shl 3) and 0x80) or (channelConfig shl 3)).toByte()
                    return ByteBuffer.wrap(csd0)
                }
                setByteBuffer(
                    "csd-0", createCodecSpecificData(
                        MediaCodecInfo.CodecProfileLevel.AACObjectLC,
                        4, 1
                    )
                )
            }
        decoder.configure(mediaFormat, null, null, 0)
    }

    private fun encode() = GlobalScope.launch(Dispatchers.IO) {
        isEos = false
        encoder.start()
        val inputChannel =
            File(getExternalFilesDir(null), "task7/sample.pcm").inputStream().channel
        val outputChannel =
            File(getExternalFilesDir(null), "task7/output.aac").outputStream().channel
        writeToMediaCodec(inputChannel, encoder, true)
        readFromMediaCodec(outputChannel, encoder, true)
    }

    private fun decode() = GlobalScope.launch(Dispatchers.IO) {
        isEos = false
        decoder.start()
        val inputStream = File(getExternalFilesDir(null), "task7/output.aac").inputStream()
        val outputChannel =
            File(getExternalFilesDir(null), "task7/output_pcm.pcm").outputStream().channel
        writeToMediaCodec(inputStream, decoder)
        readFromMediaCodec(outputChannel, decoder)
    }

    private fun writeToMediaCodec(inputStream: FileInputStream, mediaCodec: MediaCodec) {
        GlobalScope.launch(Dispatchers.IO) {
            inputStream.use { fileInputStream ->
                while (!isEos) {
                    val inputBufferIndex = mediaCodec.dequeueInputBuffer(0L)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                        val data = readAdtsFrame(fileInputStream)
                        val readCount = data.size
                        inputBuffer?.put(data)
                        isEos = readCount <= 0
                        mediaCodec.queueInputBuffer(
                            inputBufferIndex, 0,
                            if (isEos) 0 else readCount,
                            0,
                            if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else MediaCodec.BUFFER_FLAG_KEY_FRAME
                        )
                    }
                }
            }
        }
    }

    @WorkerThread
    private fun writeToMediaCodec(
        channel: FileChannel,
        mediaCodec: MediaCodec,
        isFlipEndian: Boolean = false
    ) =
        GlobalScope.launch(Dispatchers.IO) {
            channel.use { fileChannel: FileChannel ->
                while (!isEos) {
                    val inputBufferIndex = mediaCodec.dequeueInputBuffer(0L)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                        val tempBuffer = ByteBuffer.allocate(inputBuffer?.capacity() ?: 0).order(
                            ByteOrder.BIG_ENDIAN
                        )
                        val readCount =
                            fileChannel.read(if (isFlipEndian) tempBuffer else inputBuffer)
                        if (isFlipEndian) {
                            val shorts = ShortArray(readCount / 2)
                            tempBuffer.flip()
                            tempBuffer.asShortBuffer().get(shorts)
                            inputBuffer?.asShortBuffer()?.put(shorts)
                        }
                        isEos = readCount <= 0
                        mediaCodec.queueInputBuffer(
                            inputBufferIndex, 0,
                            if (isEos) 0 else readCount,
                            0,
                            if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else MediaCodec.BUFFER_FLAG_KEY_FRAME
                        )
                    }
                }
            }
        }

    @WorkerThread
    private fun readFromMediaCodec(
        channel: FileChannel,
        mediaCodec: MediaCodec,
        isNeedAdts: Boolean = false
    ) =
        GlobalScope.launch(Dispatchers.IO) {
            channel.use { fileChanel ->
                while (true) {
                    val bufferInfo = MediaCodec.BufferInfo()
                    val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0L)

                    if (bufferInfo.size <= 0 && isEos) {
                        mediaCodec.stop()
                        mediaCodec.release()
                        break
                    } else if (outputBufferIndex >= 0) {
                        val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)?.apply {
                            position(bufferInfo.offset)
                            limit(bufferInfo.size + bufferInfo.offset)
                        }
                        if (isNeedAdts) {
                            fileChanel.write(addAdtsHeader(bufferInfo.size + 7))
                        }
                        fileChanel.write(outputBuffer)
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@Task7Activity, "Finish", Toast.LENGTH_LONG).show()
                }
            }
        }

    private fun addAdtsHeader(frameLength: Int): ByteBuffer {
        val audioObjectType = 2     // MPEG-4 Audio Object Type for AAC LC
        val sampleFrequencyType = 4 // Sample Frequency Type for 44100Hz
        val channelConfig = 1      // Channel Config Type for MONO

        return ByteBuffer.allocate(7).run {
            put(0xff.toByte())
            put(0xf9.toByte())
            val profile = (audioObjectType - 1) shl 6
            val sampleFrequencyIndex = sampleFrequencyType shl 2
            val channelConfigHighest = channelConfig ushr 2
            put((profile + sampleFrequencyIndex + channelConfigHighest).toByte())
            val channelConfigRemain = (channelConfig and 3) shl 6
            val frameLengthHighest2 = frameLength ushr 11
            put((channelConfigRemain + frameLengthHighest2).toByte())
            val frameMiddleBitmask = 0b00_1111_1111_000
            put(((frameLength and frameMiddleBitmask) ushr 3).toByte())
            val frameLengthRemain = (frameLength and 7) shl 5
            put((frameLengthRemain + 0x1f).toByte())
            put(0xfc.toByte())
            position(0)
        } as ByteBuffer
    }

    enum class AacReaderState {
        // INIT is the start and final state
        INIT,
        EXPECT_ADTS,
        READ_ADTS,
        READ_DATA
    }

    private fun readAdtsFrame(inputStream: FileInputStream): ByteArray {
        var state = AacReaderState.INIT
        var current = Integer.MIN_VALUE
        lateinit var bytes: ByteArray
        loop@ while (inputStream.available() > 0) {
            when (state) {
                AacReaderState.INIT -> {
                    if (current != Integer.MIN_VALUE)
                        break@loop
                    current = inputStream.read()
                    check(current == 0xff)
                    state = AacReaderState.EXPECT_ADTS
                }
                AacReaderState.EXPECT_ADTS -> {
                    current = inputStream.read()
                    check(current and 0xf0 == 0xf0)
                    state = AacReaderState.READ_ADTS
                }
                AacReaderState.READ_ADTS -> {
                    val isHasCrc = current and 0x01 == 0
                    val readCount = (if (isHasCrc) 9 else 7) - 2
                    val remainAdtsBytes = ByteArray(readCount)
                    inputStream.read(remainAdtsBytes)
                    val dataSize =
                        (((remainAdtsBytes[1].toInt() and 0x3) shl 11) +
                                (remainAdtsBytes[2].toInt() shl 3) +
                                ((remainAdtsBytes[3].toInt() and 0xe0) ushr 5)) - (if (isHasCrc) 9 else 7)
                    current = dataSize
                    state = AacReaderState.READ_DATA
                }
                AacReaderState.READ_DATA -> {
                    bytes = ByteArray(current)
                    inputStream.read(bytes)
                    state = AacReaderState.INIT
                }
            }
        }
        check(state == AacReaderState.INIT)
        return if (current == Int.MIN_VALUE) {
            ByteArray(0)
        } else {
            bytes
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
                outputStream.close()
            }
            GlobalScope.launch(Dispatchers.Main) {
                encodeAacBtn.isEnabled = true
                decodeAacBtn.isEnabled = true
            }
        }
    }
}