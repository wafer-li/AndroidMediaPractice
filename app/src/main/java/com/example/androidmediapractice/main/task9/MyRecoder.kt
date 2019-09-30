package com.example.androidmediapractice.main.task9

import android.media.*
import android.os.SystemClock
import java.io.File
import kotlin.concurrent.thread

class MyRecorder {
    private lateinit var audioRecord: AudioRecord
    private lateinit var audioEncoder: MediaCodec
    private var bufferSize = 0

    private lateinit var mediaMuxer: MediaMuxer
    private var audioTrackIndex = -1
    private var videoTrackIndex = -1

    var isRecording = false
        private set
    private var startTime = 0L

    companion object {
        private const val AUDIO_SAMPLE_RATE = 44100
        private const val AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val AAC_BIT_RATE = 96000
    }

    fun init(file: File) {
        initAudioRecord()
        // init video record
        initMediaMuxer(file)
    }

    private fun initAudioRecord() {
        bufferSize =
            AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT)
        audioRecord =
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaFormat =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, AUDIO_SAMPLE_RATE, 1)
                .apply {
                    setInteger(MediaFormat.KEY_BIT_RATE, AAC_BIT_RATE)
                    setInteger(
                        MediaFormat.KEY_AAC_PROFILE,
                        MediaCodecInfo.CodecProfileLevel.AACObjectLC
                    )
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize * 2)
                }
        audioEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    private fun initMediaMuxer(file: File) {
        mediaMuxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    fun start() {
        isRecording = true
        startTime = SystemClock.elapsedRealtime()
        mediaMuxer.start()
        startAudioRecord()
    }

    fun stop() {
        isRecording = false
        stopAudioRecording()
        // stop video recording
        mediaMuxer.stop()
    }

    private fun startAudioRecord() {
        audioRecord.startRecording()
        audioEncoder.start()
        thread { pullingDataFromAudioRecord() }
        thread { readFromAudioEncoder() }
    }

    private fun stopAudioRecording() {
        audioRecord.stop()
        audioEncoder.stop()
    }

    fun release() {
        audioRecord.release()
        // release video record
        mediaMuxer.release()
    }

    private fun pullingDataFromAudioRecord() {
        val audioData = ShortArray(bufferSize)
        while (isRecording) {
            val result = audioRecord.read(audioData, 0, bufferSize)
            if (result > 0) {
                writeToAudioEncoder(audioData)
            }
        }
    }

    private fun writeToAudioEncoder(audioData: ShortArray) {
        val inputBufferIndex = audioEncoder.dequeueInputBuffer(0L)
        if (inputBufferIndex >= 0) {
            val inputBuffer = audioEncoder.getInputBuffer(inputBufferIndex)
                ?: error("InputBuffer error: $inputBufferIndex")
            inputBuffer.asShortBuffer().put(audioData)
            audioEncoder.queueInputBuffer(
                inputBufferIndex,
                0,
                audioData.size,
                SystemClock.elapsedRealtime() - startTime,
                0
            )
        }
    }

    private fun readFromAudioEncoder() {
        while (isRecording) {
            val bufferInfo = MediaCodec.BufferInfo()
            val outputBufferIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 0L)
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                audioTrackIndex = mediaMuxer.addTrack(audioEncoder.outputFormat)
            } else if (outputBufferIndex >= 0) {
                val outputBuffer = audioEncoder.getOutputBuffer(outputBufferIndex)
                    ?: error("OutputBufferIndex Error: $outputBufferIndex")
                mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                audioEncoder.releaseOutputBuffer(outputBufferIndex, false)
            }
        }
    }
}