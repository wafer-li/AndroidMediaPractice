package com.example.androidmediapractice.main.task3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task3.*

class Task3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task3)
        initView()
    }

    private fun initView() {
        recordVideoBtn.setOnClickListener { }
        previewSurfaceViewBtn.setOnClickListener { }
        previewTextureViewBtn.setOnClickListener { }
        nv21CallbackBtn.setOnClickListener { }
    }
}