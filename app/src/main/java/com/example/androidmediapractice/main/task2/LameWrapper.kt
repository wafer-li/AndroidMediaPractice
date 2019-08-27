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
    private val bitRate = sampleRate * 2

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

            val inBuffer = ByteArray(bufferSize * 2)
            val mp3Buffer = ByteArray(bufferSize * 2)
            val out = FileOutputStream(mp3File)
            pcmFile.inputStream().buffered().use {
                while (it.available() > 0) {
                    val readCount = it.read(inBuffer)
                    if (readCount < 0) {
                        break
                    }
                    val shorts = ShortArray(bufferSize)
                    ByteBuffer.wrap(inBuffer).asShortBuffer().get(shorts)
                    encode(shorts, null, bufferSize, mp3Buffer)
                    out.write(mp3Buffer)
                }
            }
            val flushResult = flush(mp3Buffer)
            if (flushResult > 0) {
                out.write(mp3Buffer)
            }
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