package com.example.androidmediapractice.main.task6

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.main.task5.loadShader
import org.intellij.lang.annotations.Language
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

class Squre {
    companion object {
        private val COUNT_PER_VERTEX = 3
    }

    private val vertexs = floatArrayOf(
        -1f, 1f, 0f, 0f, 1f,
        -1f, -1f, 0f, 0f, 0f,
        1f, -1f, 0f, 1f, 0f,
        1f, 1f, 0f, 1f, 1f
    )

    private val indecies = byteArrayOf(0, 1, 2, 0, 2, 3)

    @Language("GLSL")
    private val vertexShaderCode = """
        uniform mat4 uMvpMatrix;
        attribute vec3 vPosition;
        attribute vec2 vTexture;
        varying vec2 texCoord;
        
        void main() {
            gl_Position = uMvpMatrix * vec4(vPosition, 1.0);
            texCoord = vTexture;
        }
    """.trimIndent()

    @Language("GLSL")
    private val fragmentShaderCode = """
    varying texCoord;
    uniform vTexture;
    
    void main() {
        FragColor = texture(vTexture, texCoord);
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
        glBufferData(
            GL_ARRAY_BUFFER, vertexs.size * 4, ByteBuffer.allocateDirect(vertexs.size * 4).apply {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(vertexs)
                    position(0)
                }
            },
            GL_STATIC_DRAW
        )

        // EBO
        glGenBuffers(1, ebos, 0)
        val ebo = ebos[0]
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glBufferData(
            GL_ELEMENT_ARRAY_BUFFER,
            indecies.size,
            ByteBuffer.allocateDirect(indecies.size).apply {
                order(ByteOrder.nativeOrder())
                put(indecies)
                position(0)
            }, GL_STATIC_DRAW
        )
    }

    fun draw(mvpMatrix: FloatArray) {

    }
}
