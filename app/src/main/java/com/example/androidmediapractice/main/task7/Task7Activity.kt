package com.example.androidmediapractice.main.task7

import android.media.MediaCodec
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R

class Task7Activity : AppCompatActivity() {
    private lateinit var encoder: MediaCodec
    private lateinit var decoder: MediaCodec

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task7)
    }
}