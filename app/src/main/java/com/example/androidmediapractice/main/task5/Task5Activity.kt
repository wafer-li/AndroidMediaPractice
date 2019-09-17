package com.example.androidmediapractice.main.task5

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Task5Activity : AppCompatActivity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var triangle: Triangle
    private val renderer = object : GLSurfaceView.Renderer {
        override fun onDrawFrame(p0: GL10?) {
            triangle.draw()
        }

        override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
            glViewport(0, 0, p1, p2)
        }

        override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
            triangle = Triangle()
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

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    class Triangle {
        companion object {
            private const val COUNT_PER_VERTEX = 3
            private val triangleCoords = floatArrayOf(
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                0.0f, 0.5f, 0.0f
            )
        }

        private val vertexBuffer: FloatBuffer =
            ByteBuffer.allocateDirect(triangleCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(triangleCoords)
                    position(0)
                }
            }

        private val color = floatArrayOf(1.0f, 0.5f, 0.2f, 1.0f)

        private val vertexShaderCode = """
            attribute vec4 vPosition;
            void main() {
                gl_Position = vPosition;
            }
        """.trimIndent()

        private val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """.trimIndent()

        private val vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderCode)
        private val fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode)

        private val program = glCreateProgram().also {
            glAttachShader(it, vertexShader)
            glAttachShader(it, fragmentShader)

            glLinkProgram(it)

            glDetachShader(it, vertexShader)
            glDetachShader(it, fragmentShader)
        }

        private val vertexCount = triangleCoords.size / COUNT_PER_VERTEX
        private val vertexStride = COUNT_PER_VERTEX * 4

        fun draw() {
            glUseProgram(program)

            glGetAttribLocation(program, "vPosition").also {
                glVertexAttribPointer(
                    it,
                    COUNT_PER_VERTEX,
                    GL_FLOAT,
                    false,
                    vertexStride,
                    vertexBuffer
                )
                glEnableVertexAttribArray(it)

                glGetUniformLocation(program, "vColor").also { colorHandle ->
                    glUniform4fv(colorHandle, 1, color, 0)
                }

                glDrawArrays(GL_TRIANGLES, 0, vertexCount)
                glDisableVertexAttribArray(it)
            }
        }
    }
}

private fun loadShader(type: Int, shaderCode: String): Int {
    return glCreateShader(type).also {
        glShaderSource(it, shaderCode)
        glCompileShader(it)
    }
}
