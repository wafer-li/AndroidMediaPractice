package com.example.androidmediapractice.main.task4

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.activity_task4.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class Task4Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task4)
        prepareSampleMp4()
        mediaExtractorBtn.setOnClickListener {
            startActivity(Intent(this, MediaExtractorActivity::class.java))
        }
    }

    private fun prepareSampleMp4() = GlobalScope.launch(Dispatchers.IO) {
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
