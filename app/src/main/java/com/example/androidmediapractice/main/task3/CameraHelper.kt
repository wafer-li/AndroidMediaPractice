package com.example.androidmediapractice.main.task3

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class CameraHelper(
    private val context: Context,
    private val textureView: TextureView? = null,
    private val surfaceView: SurfaceView? = null
) : LifecycleObserver {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraCharacteristics: CameraCharacteristics
    private var cameraId: String = ""

    private lateinit var imageReader: ImageReader

    private var cameraOrientation = 0
    private val displayRotation =
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation

    private val handlerThread = HandlerThread("CameraThread")
    private val cameraHandler: Handler

    private var cameraCaptureSession: CameraCaptureSession? = null

    private var canTakePic = true
    private var canExchangeCamera = false

    var isRecording = false
        private set

    private lateinit var outputStream: FileOutputStream

    private var previewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)


    companion object {
        private const val CAMERA_FACING = CameraCharacteristics.LENS_FACING_BACK
        private const val PREVIEW_WIDTH = 1080
        private const val PREVIEW_HEIGHT = 1920
    }

    init {
        handlerThread.start()
        cameraHandler = Handler(handlerThread.looper)

        if (textureView != null) {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
                }

                override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
                }

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
                    close()
                    return true
                }

                override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
                    init()
                }
            }
        } else surfaceView?.holder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
            }

            override fun surfaceDestroyed(p0: SurfaceHolder?) {
                close()
            }

            override fun surfaceCreated(p0: SurfaceHolder?) {
                init()
            }

        })
    }

    fun init() {
        // Obtain camera id and characteristic
        for (it in cameraManager.cameraIdList) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(it)
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CAMERA_FACING) {
                this.cameraCharacteristics = cameraCharacteristics
                this.cameraId = it
                break
            }
        }

        // Obtain camera orientation
        this.cameraOrientation =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

        val isExchange = isExchangeWidthHeight(displayRotation, cameraOrientation)

        previewSize = Size(
            if (isExchange) PREVIEW_HEIGHT else PREVIEW_WIDTH,
            if (isExchange) PREVIEW_WIDTH else PREVIEW_HEIGHT
        )

        // Setup preview
        textureView?.surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)

        imageReader = ImageReader.newInstance(
            previewSize.width,
            previewSize.height,
            ImageFormat.YUV_420_888,
            1
        )

        openCamera()
    }


    @SuppressLint("MissingPermission")
    private fun openCamera() {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "无权限", Toast.LENGTH_SHORT).show()
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(cameraDevice: CameraDevice) {
                this@CameraHelper.cameraDevice = cameraDevice
                createCaptureSession(cameraDevice)
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {
            }

            override fun onError(cameraDevice: CameraDevice, p1: Int) {
            }
        }, cameraHandler)
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        init()
    }

    private fun createCaptureSession(cameraDevice: CameraDevice?) {

        if (cameraDevice == null) {
            return
        }

        val surface = when {
            textureView != null -> Surface(textureView.surfaceTexture)
            surfaceView != null -> surfaceView.holder.surface
            else -> error("Surface 错误")
        }

        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        captureRequestBuilder.set(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO
        )
        captureRequestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )

        cameraDevice.createCaptureSession(
            arrayListOf(surface, imageReader.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(p0: CameraCaptureSession) {
                    Toast.makeText(context, "开启预览会话失败", Toast.LENGTH_SHORT).show()
                }

                override fun onConfigured(p0: CameraCaptureSession) {
                    cameraCaptureSession = p0
                    p0.setRepeatingRequest(
                        captureRequestBuilder.build(),
                        captureCallback,
                        cameraHandler
                    )
                }
            }, cameraHandler
        )
    }


    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            canExchangeCamera = true
            canTakePic = true
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            Toast.makeText(context, "预览失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun takePicture() {

    }

    fun startRecord(file: File) {
        if (!isRecording) {
            isRecording = true
            outputStream = file.outputStream()
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                val byteBuffers = image.planes.map {
                    it.buffer.duplicate()
                }
                image.close()
                GlobalScope.launch(Dispatchers.IO) {
                    byteBuffers.forEach {
                        outputStream.channel.write(it)
                    }
                }
            }, cameraHandler)
            cameraDevice.apply {
                val surface = when {
                    textureView != null -> Surface(textureView.surfaceTexture)
                    surfaceView != null -> surfaceView.holder.surface
                    else -> error("Surface 错误")
                }
                val request = this.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                request.addTarget(surface)
                request.addTarget(imageReader.surface)
                request.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO
                )
                request.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                request.build().let {
                    cameraCaptureSession?.setRepeatingRequest(
                        it,
                        object : CameraCaptureSession.CaptureCallback() {},
                        cameraHandler
                    )
                }
            }
        }
    }

    fun stopRecord() {
        if (isRecording) {
            cameraCaptureSession?.close()
            createCaptureSession(cameraDevice)

            outputStream.close()
            imageReader.close()

            isRecording = false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun close() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null

        cameraDevice.close()

        outputStream.close()

        imageReader.close()

        canExchangeCamera = false
    }

    private fun isExchangeWidthHeight(
        displayOrientation: Int,
        cameraSensorOrientation: Int
    ): Boolean {
        return when (displayOrientation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                cameraSensorOrientation == 90 || cameraSensorOrientation == 270
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                cameraSensorOrientation == 0 || cameraSensorOrientation == 180
            }
            else -> false
        }
    }
}
