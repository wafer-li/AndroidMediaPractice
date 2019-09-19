package com.example.androidmediapractice.main.task6

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.main.task5.loadShader
import org.intellij.lang.annotations.Language
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Task6Activity : AppCompatActivity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)
    private lateinit var square: Square
    private val renderer = object : GLSurfaceView.Renderer {
        override fun onDrawFrame(p0: GL10?) {
            Matrix.setLookAtM(
                viewMatrix, 0,
                0f, 0f, -3f,
                0f, 0f, 0f,
                0f, 1f, 0f
            )

            Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            square.draw(vPMatrix)
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
            square = Square()
            square.init()
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

class Square {
    companion object {
        // number of coordinates per vertex in this array
        const val COORDS_PER_VERTEX = 3
        var squareCoords = floatArrayOf(
            -0.5f, 0.5f, 0.0f,      // top left
            -0.5f, -0.5f, 0.0f,      // bottom left
            0.5f, -0.5f, 0.0f,      // bottom right
            0.5f, 0.5f, 0.0f       // top right
        )

        private val color = floatArrayOf(1.0f, 0.5f, 0.2f, 1.0f)
    }

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

    // initialize vertex byte buffer for shape coordinates
    private val vertexBuffer: FloatBuffer =
        // (# of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(squareCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(squareCoords)
                position(0)
            }
        }

    // initialize byte buffer for the draw list
    private val drawListBuffer: ShortBuffer =
        // (# of coordinate values * 2 bytes per short)
        ByteBuffer.allocateDirect(drawOrder.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(drawOrder)
                position(0)
            }
        }

    @Language("GLSL")
    private val vertexShaderCode =
        """
    uniform mat4 uMvpMatrix;
            attribute vec4 vPosition; 
                void main() {
                  gl_Position = uMvpMatrix * vPosition;
                }""".trimIndent()

    @Language("GLSL")
    private val fragmentShaderCode =
        """precision mediump float; 
                uniform vec4 vColor;
                void main() {
                  gl_FragColor = vColor;
                }""".trimIndent()

    private val vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderCode)
    private val fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode)

    private val program = glCreateProgram().also {
        glAttachShader(it, vertexShader)
        glAttachShader(it, fragmentShader)
        glLinkProgram(it)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    private var vbo = 0
    private var ebo = 0

    fun init() {
        val buffers = IntArray(2)
        glGenBuffers(2, buffers, 0)
        vbo = buffers[0]
        ebo = buffers[1]

        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glBufferData(
            GL_ELEMENT_ARRAY_BUFFER,
            drawListBuffer.capacity() * 2,
            drawListBuffer,
            GL_STATIC_DRAW
        )
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun draw(mvpMatrix: FloatArray) {
        glUseProgram(program)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        val positionHandle = glGetAttribLocation(program, "vPosition")
        glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,
            GL_FLOAT,
            false,
            COORDS_PER_VERTEX * 4,
            0
        )
        glEnableVertexAttribArray(positionHandle)

        glGetUniformLocation(program, "vColor").also { colorHandle ->
            glUniform4fv(colorHandle, 1, color, 0)
        }

        glGetUniformLocation(program, "uMvpMatrix").also { uMvpMatrixHandle ->
            glUniformMatrix4fv(uMvpMatrixHandle, 1, false, mvpMatrix, 0)
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_SHORT, 0)
        glDisableVertexAttribArray(positionHandle)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }
}