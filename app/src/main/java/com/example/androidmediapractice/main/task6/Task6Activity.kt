package com.example.androidmediapractice.main.task6

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.main.task5.loadShader
import org.intellij.lang.annotations.Language
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
            square = Squre()
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

class Squre {
    companion object {
        private val COUNT_PER_VERTEX = 3
    }

    private val vertexs = floatArrayOf(
        0.5f, 0.5f, 0.0f,   // 右上角
        0.5f, -0.5f, 0.0f,  // 右下角
        -0.5f, -0.5f, 0.0f, // 左下角
        -0.5f, 0.5f, 0.0f   // 左上角
    )

    private val indecies = intArrayOf(
        0, 1, 3, // 第一个三角形
        1, 2, 3  // 第二个三角形
    )

    @Language("GLSL")
    private val vertexShaderCode = """
        uniform mat4 uMvpMatrix;
        attribute vec3 vPosition;
        
        void main() {
            gl_Position = uMvpMatrix * vec4(vPosition, 1.0);
        }
    """.trimIndent()

    @Language("GLSL")
    private val fragmentShaderCode = """
    
    void main() {
        FragColor = vec4(1.0, 0.0, 0.0, 1.0);
    }
    """.trimIndent()

    private val vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderCode)
    private val fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode)
    private val program = glCreateProgram().also {
        glAttachShader(it, vertexShader)
        glAttachShader(it, fragmentShader)

        glLinkProgram(it)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    private val vbos = IntArray(1)
    private val ebos = IntArray(1)

    fun init() {
        // VBO
        glGenBuffers(1, vbos, 0)
        val vbo = vbos[0]
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        val vertexBuffer = ByteBuffer.allocateDirect(vertexs.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertexs)
                position(0)
            }
        }

        glBufferData(
            GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer,
            GL_STATIC_DRAW
        )
        glGetAttribLocation(program, "vPosition").also {
            glVertexAttribPointer(it, 3, GL_FLOAT, false, 12, 0)
            Log.d("glError", glGetError().toString())
            glEnableVertexAttribArray(it)
        }


        // EBO
        glGenBuffers(1, ebos, 0)
        val ebo = ebos[0]
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        val indexBuffer = ByteBuffer.allocateDirect(indecies.size * Integer.SIZE / 8).run {
            order(ByteOrder.nativeOrder())
            asIntBuffer().apply {
                put(indecies)
                position(0)
            }
        }
        glBufferData(
            GL_ELEMENT_ARRAY_BUFFER,
            indexBuffer.capacity() * Integer.SIZE / 8,
            indexBuffer, GL_STATIC_DRAW
        )
    }

    fun draw(mvpMatrix: FloatArray) {
        glUseProgram(program)
        val vbo = vbos[0]
        val ebo = ebos[0]
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)

        glGetUniformLocation(program, "uMvpMatrix").also {
            glUniformMatrix4fv(it, 1, false, mvpMatrix, 0)
        }

        glDrawElements(GL_TRIANGLES, indecies.size, GL_UNSIGNED_INT, 0)
    }
}
