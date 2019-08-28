package com.example.androidmediapractice.main.task2

import android.media.AudioFormat
import android.media.AudioRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class LameWrapper(pcmFilePath: String, mp3FilePath: String) {

    private val pcmFile = File(pcmFilePath)
    private val mp3File = File(mp3FilePath)

    private val sampleRate = 44100
    private val channels = 1
    private val quality = 7
    private val bitRate = 32

    private val bufferSize =
        AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

    init {
        System.loadLibrary("native-lib")
    }

    fun toMp3File(callback: () -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            init(sampleRate, sampleRate, channels, bitRate, quality)

            val out = FileOutputStream(mp3File)
            val inputStream = pcmFile.inputStream().buffered()
            val inBuffer = inputStream.readBytes()
            val shorts = ShortArray(inBuffer.size / 2)
            val mp3Buffer = ByteArray(inBuffer.size)
            ByteBuffer.wrap(inBuffer).asShortBuffer().get(shorts)
            encode(shorts, null, shorts.size, mp3Buffer)
            out.write(mp3Buffer.dropLastWhile { it == 0.toByte() }.toByteArray())
            inputStream.close()
            out.close()
            close()
            GlobalScope.launch(Dispatchers.Main) {
                callback()
            }
        }
    }

    private external fun init(
        inSampleRate: Int,
        outSampleRate: Int,
        outChannels: Int,
        outBitRates: Int,
        quality: Int
    )

    external fun encode(
        bufferLeft: ShortArray,
        bufferRight: ShortArray?,
        samples: Int,
        mp3Buffer: ByteArray
    )

    private external fun flush(
        mp3Buffer: ByteArray
    ): Int

    private external fun close()
}