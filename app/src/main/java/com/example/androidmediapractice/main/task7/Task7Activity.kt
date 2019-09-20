package com.example.androidmediapractice.main.task7

import android.Manifest
import android.media.MediaCodec
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task7.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File

@RuntimePermissions
class Task7Activity : AppCompatActivity() {
    private lateinit var encoder: MediaCodec
    private lateinit var decoder: MediaCodec

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