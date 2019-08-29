package com.example.androidmediapractice.main.task3

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.surface_capture_view.*

class CaptureActivity : AppCompatActivity() {

    private lateinit var cameraHelper: CameraHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.surface_capture_view)
        initView()
        cameraHelper = CameraHelper(this, surfaceView = captureSurfaceView)
    }

    private fun initView() {
        captureBtn.setOnClickListener {
            Toast.makeText(this, "Capture", Toast.LENGTH_SHORT).show()
        }
    }
}