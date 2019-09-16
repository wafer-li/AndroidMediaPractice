package com.example.androidmediapractice.main.task4

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task4.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File

@RuntimePermissions
class Task4Activity : AppCompatActivity() {
    private lateinit var muxHelper: MediaMuxHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task4)
        prepareSampleMp4WithPermissionCheck()
        mediaExtractorBtn.setOnClickListener {
            startActivity(Intent(this, MediaExtractorActivity::class.java))
        }
        mediaMuxerBtn.setOnClickListener {
            if (!this::muxHelper.isInitialized) {
                muxHelper = MediaMuxHelper(
                    File(getExternalFilesDir(null), "sample.mp4").absolutePath,
                    File(getExternalFilesDir(null), "output.mp4").absolutePath
                )
            }
            muxHelper.mux(this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::muxHelper.isInitialized) {
            muxHelper.release()
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun prepareSampleMp4() {
        GlobalScope.launch(Dispatchers.IO) {
            val file = File(getExternalFilesDir(null), "sample.mp4")
            val isPrepared = file.exists()
            if (!isPrepared) {
                val outputStream = file.outputStream()
                resources.assets.open("sample.mp4").use {
                    while (it.available() > 0) {
                        outputStream.write(it.read())
                    }
                }
                outputStream.close()
            }
            GlobalScope.launch(Dispatchers.Main) {
                mediaExtractorBtn.isEnabled = true
                mediaMuxerBtn.isEnabled = true
            }
        }
    }
}
