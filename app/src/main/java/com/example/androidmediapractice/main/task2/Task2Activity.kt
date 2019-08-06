package com.example.androidmediapractice.main.task2

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task2.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@RuntimePermissions
class Task2Activity : AppCompatActivity() {
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordBufSize = 0

    private val isRecording: AtomicBoolean = AtomicBoolean(false)
    private val isPlaying: AtomicBoolean = AtomicBoolean(false)
    private var count = 0

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
                    getString(R.string.audio_start_play)
                } else {
                    getString(R.string.audio_stop_play)
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
            recordBufSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                ENCODING_FORMAT
            )
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

    private fun obtainFileName() = "record$count.pcm"

    @WorkerThread
    private fun pullingRecordData() {
        val audioData = ShortArray(recordBufSize)
        val out = FileOutputStream(File(getExternalFilesDir(null), obtainFileName()))
        out.use { fileOut ->
            while (isRecording.get()) {
                val status = audioRecord?.read(audioData, 0, recordBufSize) ?: -10
                if (status >= 0) {
                    Log.i("Write PCM File", audioData.size.toString())
                    fileOut.write(audioData.map { it.toByte() }.toByteArray())
                }
            }
        }
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun startPlay() {
        disableRecord()
        isPlaying.set(true)
    }

    private fun stopPlay() {
        enableRecord()
        isPlaying.set(false)
    }
}