package com.example.androidmediapractice.main.task6

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.example.androidmediapractice.R
import com.example.androidmediapractice.main.task5.loadShader
import org.intellij.lang.annotations.Language
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
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
            square.init(this@Task6Activity)
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
        const val COORDS_PER_VERTEX = 5

    }

    private val squareCoords = floatArrayOf(
        -0.5f, 0.5f, 0.0f, 0f, 1f,     // top left
        -0.5f, -0.5f, 0.0f, 0f, 0f,     // bottom left
        0.5f, -0.5f, 0.0f, 1f, 0f,    // bottom right
        0.5f, 0.5f, 0.0f, 1f, 1f   // top right
    )

    private val color = floatArrayOf(1.0f, 0.5f, 0.2f, 1.0f)

    private val drawOrder = intArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices


    @Language("GLSL")
    private val vertexShaderCode =
        """
    uniform mat4 uMvpMatrix;
            attribute vec3 vPosition; 
            attribute vec2 vTexCoord;
            varying vec2 texCoord;
                void main() {
                  texCoord = vec2(vTexCoord.x, 1.0 - vTexCoord.y);
                  gl_Position = uMvpMatrix * vec4(vPosition,1.0);
                }""".trimIndent()

    @Language("GLSL")
    private val fragmentShaderCode =
        """precision mediump float; 
                uniform vec4 vColor;
                uniform sampler2D ourTexture;
                varying vec2 texCoord;
                void main() {
                  gl_FragColor = texture2D(ourTexture, texCoord); 
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

    private var texture = 0

    fun init(context: Context) {
        val buffers = IntArray(2)
        glGenBuffers(2, buffers, 0)
        val vbo = buffers[0]
        val ebo = buffers[1]

        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        // initialize vertex byte buffer for shape coordinates
        val vertexBuffer: FloatBuffer =
            // (# of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(squareCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(squareCoords)
                    position(0)
                }
            }

        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GL_STATIC_DRAW)

        val positionHandle = glGetAttribLocation(program, "vPosition")
        glVertexAttribPointer(
            positionHandle,
            3,
            GL_FLOAT,
            false,
            COORDS_PER_VERTEX * 4,
            0
        )
        glEnableVertexAttribArray(positionHandle)

        val texCoordHandle = glGetAttribLocation(program, "vTexCoord")
        glVertexAttribPointer(
            texCoordHandle,
            2,
            GL_FLOAT,
            false,
            COORDS_PER_VERTEX * 4,
            3 * 4
        )
        glEnableVertexAttribArray(texCoordHandle)

        // EBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        val drawListBuffer: IntBuffer =
            // (# of coordinate values * 2 bytes per short)
            ByteBuffer.allocateDirect(drawOrder.size * 4).run {
                order(ByteOrder.nativeOrder())
                asIntBuffer().apply {
                    put(drawOrder)
                    position(0)
                }
            }

        glBufferData(
            GL_ELEMENT_ARRAY_BUFFER,
            drawListBuffer.capacity() * 4,
            drawListBuffer, GL_STATIC_DRAW
        )

        // Generate texture
        val textures = IntArray(1)
        glGenTextures(textures.size, textures, 0)
        texture = textures[0]

        // Load Image
        val bitmap =
            context.getDrawable(R.mipmap.ic_launcher)?.toBitmap() ?: error("Load Bitmap Error")
        val bitmapBuffer =
            ByteBuffer.allocateDirect(bitmap.byteCount).order(ByteOrder.nativeOrder())
        bitmap.copyPixelsToBuffer(bitmapBuffer)
        bitmapBuffer.position(0)

        glBindTexture(GL_TEXTURE_2D, texture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            bitmap.width,
            bitmap.height,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            bitmapBuffer
        )

        bitmap.recycle()
    }

    fun draw(mvpMatrix: FloatArray) {
        glUseProgram(program)

        glGetUniformLocation(program, "vColor").also { colorHandle ->
            glUniform4fv(colorHandle, 1, color, 0)
        }

        glGetUniformLocation(program, "uMvpMatrix").also { uMvpMatrixHandle ->
            glUniformMatrix4fv(uMvpMatrixHandle, 1, false, mvpMatrix, 0)
        }

        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_INT, 0)
    }
}