package com.example.androidmediapractice.main.task9

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task9.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File

@RuntimePermissions
class Task9Activity : AppCompatActivity() {
    private val myRecorder = MyRecorder()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task9)
        initMyRecorderWithPermissionCheck()
        captureBtn.setOnClickListener {
            if (myRecorder.isRecording) {
                myRecorder.stop()
            } else {
                myRecorder.start()
            }
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun initMyRecorder() {
        myRecorder.init(obtainMp4File())
    }

    override fun onResume() {
        super.onResume()
        myRecorder.start()
    }

    override fun onPause() {
        super.onPause()
        myRecorder.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        myRecorder.release()
    }

    private fun obtainMp4File() = File(getExternalFilesDir(null), "record_output.mp4")
}