package com.example.androidmediapractice.main.task6

import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Task6Activity : AppCompatActivity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)
    private lateinit var square: Squre
    private val renderer = object : GLSurfaceView.Renderer {
        override fun onDrawFrame(p0: GL10?) {
            Matrix.setLookAtM(
                viewMatrix, 0,
                0f, 0f, -3f,
                0f, 0f, 0f,
                0f, 1f, 0f
            )

            Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        }

        override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
            glViewport(0, 0, width, height)
            val ratio: Float = if (width > height)
                width.toFloat() / height.toFloat()
            else height.toFloat() / width.toFloat()
            if (width > height) {
                Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
            } else {
                Matrix.frustumM(projectionMatrix, 0, -1f, 1f, -ratio, ratio, 3f, 7f)
            }
        }

        override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
            square = Squre()
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

class Squre
