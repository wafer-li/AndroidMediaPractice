package com.example.androidmediapractice.main.task4

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task4.*

class Task4Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task4)
        mediaExtractorBtn.setOnClickListener {
            startActivity(Intent(this, MediaExtractorActivity::class.java))
        }
    }
}