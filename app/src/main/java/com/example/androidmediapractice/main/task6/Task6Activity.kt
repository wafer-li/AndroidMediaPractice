package com.example.androidmediapractice.main.task6

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Task6Activity : AppCompatActivity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private val renderer = object : GLSurfaceView.Renderer {
        override fun onDrawFrame(p0: GL10?) {
        }

        override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
        }

        override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
        }
        setContentView(glSurfaceView)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
}