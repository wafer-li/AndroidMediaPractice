package com.example.androidmediapractice.main.task2

import android.Manifest
import android.media.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task2.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread


@RuntimePermissions
class Task2Activity : AppCompatActivity() {
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private val recordBufSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        ENCODING_FORMAT
    )

    private val playBufSize = AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        ENCODING_FORMAT
    )

    private val isRecording: AtomicBoolean = AtomicBoolean(false)
    private val isPlaying: AtomicBoolean = AtomicBoolean(false)

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT
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
        setContentView(R.layout.activity_task2)
        initView()
    }

    private fun initView() {
        recordBtn.setOnClickListener {
            if (isRecording.get()) {
                stopRecord()
            } else {
                startRecordWithPermissionCheck()
            }

            recordBtn.text =
                if (isRecording.get()) {
                    getString(R.string.audio_stop_record)
                } else {
                    getString(R.string.audio_start_record)
                }
        }
        playBtn.setOnClickListener {
            if (isPlaying.get()) {
                stopPlay()
            } else {
                startPlayWithPermissionCheck()
            }

            playBtn.text =
                if (isPlaying.get()) {
                    getString(R.string.audio_stop_play)
                } else {
                    getString(R.string.audio_start_play)
                }
        }
        saveWavBtn.setOnClickListener {
            pcmToWav(File(getExternalFilesDir(null), obtainFileName()))
        }
        playWavBtn.setOnClickListener {
            playWavFile(File(getExternalFilesDir(null), obtainFileName().replace("pcm", "wav")))
        }
        encodeMp3Btn.setOnClickListener {
            val pcmFullPath = File(getExternalFilesDir(null), obtainFileName()).absolutePath
            val mp3FullPath = File(getExternalFilesDir(null), obtainMp3FilePath()).absolutePath
            GlobalScope.launch(Dispatchers.IO) {
                LameWrapper().run {
                    init(pcmFullPath, 1, SAMPLE_RATE * 2, SAMPLE_RATE, mp3FullPath)
                    encode()
                    destroy()
                    GlobalScope.launch(Dispatchers.Main) {
                        Toast.makeText(this@Task2Activity, "mp3 成功", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun enableRecord() {
        recordBtn.isEnabled = true
    }

    private fun disableRecord() {
        recordBtn.isEnabled = false
    }

    private fun enablePlay() {
        playBtn.isEnabled = true
    }

    private fun disablePlay() {
        playBtn.isEnabled = false
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun startRecord() {
        disablePlay()
        if (audioRecord == null) {

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG,
                ENCODING_FORMAT, recordBufSize
            )
        }
        isRecording.set(true)
        audioRecord?.startRecording()
        thread(start = true) { pullingRecordData() }
    }

    private fun stopRecord() {
        enablePlay()
        isRecording.set(false)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun obtainFileName() = "record0.pcm"
    private fun obtainMp3FilePath() = "record0.mp3"

    @WorkerThread
    private fun pullingRecordData() {
        val audioData = ShortArray(recordBufSize)
        val out = DataOutputStream(
            FileOutputStream(
                File(
                    getExternalFilesDir(null),
                    obtainFileName()
                )
            ).buffered()
        )
        out.use { fileOut ->
            while (isRecording.get()) {
                val status = audioRecord?.read(audioData, 0, recordBufSize) ?: -10
                if (status >= 0) {
                    Log.i("Write PCM File", audioData.size.toString())
                    audioData.forEach { fileOut.writeShort(it.toInt()) }
                }
            }
        }
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun startPlay() {
        disableRecord()
        isPlaying.set(true)
        if (audioTrack == null) {
            audioTrack = AudioTrack(
                AudioAttributes.Builder().build(),
                AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(ENCODING_FORMAT)
                    .build(),
                playBufSize,
                AudioTrack.MODE_STREAM,
                0
            )
        }

        audioTrack?.play()
        thread(start = true) { pushingPlayData() }
    }

    @WorkerThread
    private fun pushingPlayData() {
        val shorts = ShortArray(playBufSize)
        val bytes = ByteArray(playBufSize * 2)
        DataInputStream(
            File(
                getExternalFilesDir(null),
                obtainFileName()
            ).inputStream().buffered()
        ).use { input ->
            while (isPlaying.get() && input.available() > 0) {
                input.read(bytes)
                ByteBuffer.wrap(bytes).asShortBuffer().get(shorts)
                val playStatus = audioTrack?.write(shorts, 0, playBufSize) ?: -10
                if (playStatus < 0) {
                    break
                }
            }
            stopPlay()
        }
    }

    private fun stopPlay() {
        isPlaying.set(false)
        GlobalScope.launch(Dispatchers.Main) {
            enableRecord()
            playBtn.text = getString(R.string.audio_start_play)
        }
        if (audioTrack?.playState != AudioTrack.PLAYSTATE_STOPPED) {
            audioTrack?.stop()
        }
        audioTrack?.release()
        audioTrack = null
    }

    private fun pcmToWav(file: File) {
        thread(start = true) {
            if (file.exists() && file.isFile && file.extension == "pcm") {
                val wavFile = File(getExternalFilesDir(null), "${file.nameWithoutExtension}.wav")
                DataOutputStream(wavFile.outputStream().buffered()).use {
                    it.writeWavHeader(file)
                }
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@Task2Activity, "Save to WAV Successful", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun DataOutputStream.writeWavHeader(pcmFile: File) {
        val rawData = pcmFile.readBytes()
        /* The RIFF Chunk descriptor */
        write("RIFF".toByteArray())      // RIFF ChunkId
        writeInt((36 + rawData.size).reverseBytes()) // ChunkSize
        write("WAVE".toByteArray())      // Format

        /* The fmt sub-chunk */
        write("fmt ".toByteArray()) // Subchunk1ID
        writeInt(16.reverseBytes()) // Subchunk1Size
        writeShortInverted(1) // AudioFormat, PCM = 1, other indicate some form of compression
        writeShortInverted(1) // Number of channels, MONO = 1 , STEREO = 2
        writeInt(SAMPLE_RATE.reverseBytes())   // Sample Rate, in Hz
        writeInt((SAMPLE_RATE * 1 * (16 / 8)).reverseBytes()) // Bitrate = Sample Rate * NumChannels * BitPerSample / 8
        writeShortInverted((1 * 16 / 8)) // BlockAlign = NumChannels * BitsPerSample/8
        writeShortInverted(16)     // BitsPerSample

        /* The data sub-chunk */
        write("data".toByteArray()) // Subchunk2ID
        // Subchunk2Size = NumSamples * NumChannels * BitsPerSample/8 = The size of PCM data
        writeInt(pcmFile.length().toInt().reverseBytes())
        // PCM data, using little-endian
        val shorts = ShortArray(rawData.size / 2)
        ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        shorts.forEach { this.writeShort(it.toInt()) }
    }

    private fun Int.reverseBytes(): Int {
        val v0 = (this ushr 0) and 0xFF
        val v1 = (this ushr 8) and 0xFF
        val v2 = (this ushr 16) and 0xFF
        val v3 = (this ushr 24) and 0xFF
        return (v0 shl 24) or (v1 shl 16) or (v2 shl 8) or (v3 shl 0)
    }

    private fun DataOutputStream.writeShortInverted(value: Int) {
        val v0 = value and 0xFF
        val v1 = (value ushr 8) and 0xFF
        write(v0)
        write(v1)
    }

    private fun playWavFile(file: File) {
        if (file.exists() && file.isFile && file.extension == "wav" && file.length() > 44) {
            playWavBtn.isEnabled = false
            saveWavBtn.isEnabled = false
            disablePlay()
            disableRecord()
            isPlaying.set(true)
            if (audioTrack == null) {
                audioTrack = AudioTrack(
                    AudioAttributes.Builder().build(),
                    AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(ENCODING_FORMAT)
                        .build(),
                    playBufSize,
                    AudioTrack.MODE_STREAM,
                    0
                )
            }
            audioTrack?.play()
            thread(start = true) {
                val raf = RandomAccessFile(file, "r")
                raf.seek(44L)
                val shorts = ShortArray(playBufSize)
                val bytes = ByteArray(playBufSize * 2)
                raf.use {
                    while (isPlaying.get()) {
                        val readCount = it.read(bytes)
                        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            .get(shorts)
                        if (readCount < 0) {
                            break
                        }
                        val playStatus = audioTrack?.write(shorts, 0, playBufSize) ?: -10
                        if (playStatus < 0) {
                            break
                        }
                    }
                }
                isPlaying.set(false)
                GlobalScope.launch(Dispatchers.Main) {
                    playWavBtn.isEnabled = true
                    saveWavBtn.isEnabled = true
                    enablePlay()
                    enableRecord()
                }
            }
        }
    }
}