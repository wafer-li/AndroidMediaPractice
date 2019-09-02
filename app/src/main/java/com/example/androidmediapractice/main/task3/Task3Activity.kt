package com.example.androidmediapractice.main.task3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task3.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class Task3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task3)
        initView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun initView() {
        recordVideoBtn.setOnClickListener {
            openRecordWithPermissionCheck()
        }
        previewSurfaceViewBtn.setOnClickListener {
            startActivity(Intent(this, PreviewSurfaceActivity::class.java))
        }
        previewTextureViewBtn.setOnClickListener { }
        nv21CallbackBtn.setOnClickListener { }
    }

    @NeedsPermission(android.Manifest.permission.CAMERA)
    internal fun openRecord() {
        startActivity(Intent(this, CaptureActivity::class.java))
    }
}