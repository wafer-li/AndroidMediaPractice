package com.example.androidmediapractice.main.task3

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.surface_capture_view.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File

@RuntimePermissions
class CaptureActivity : AppCompatActivity() {

    private lateinit var cameraHelper: CameraHelper

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
        setContentView(R.layout.surface_capture_view)
        initViewWithPermissionCheck()
        cameraHelper = CameraHelper(this, surfaceView = captureSurfaceView)
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun initView() {
        captureBtn.setOnClickListener {
            if (cameraHelper.isRecording)
                cameraHelper.stopRecord()
            else cameraHelper.startRecord(obtainFile())
        }
    }

    private fun obtainFile(): File {
        return File(getExternalFilesDir(null), "yuv420_888.yuv")
    }
}