package com.example.androidmediapractice.main.task2

class LameWrapper {

    init {
        System.loadLibrary("native-lib")
    }

    external fun init(
        pcmPath: String,
        audioChannels: Int,
        bitRate: Int,
        sampleRate: Int,
        mp3Path: String
    )
    external fun encode()
    external fun destroy()
}