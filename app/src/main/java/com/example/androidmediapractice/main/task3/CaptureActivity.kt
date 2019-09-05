package com.example.androidmediapractice.main.task3

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediapractice.R
import kotlinx.android.synthetic.main.surface_capture_view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.io.FileOutputStream
import java.lang.Long.signum
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@RuntimePermissions
class CaptureActivity : AppCompatActivity() {
    private var cameraDevice: CameraDevice? = null

    private var captureSession: CameraCaptureSession? = null

    private lateinit var previewSize: Size
    private lateinit var videoSize: Size

    private var isRecordingVideo = false

    private var backgroundThread: HandlerThread? = null

    private var backgroundHandler: Handler? = null

    private val cameraOpenCloseLock: Semaphore = Semaphore(1)

    private lateinit var previewRequestBuilder: CaptureRequest.Builder

    private var sensorOrientation = 0

    private var cameraFacing = CameraCharacteristics.LENS_FACING_BACK

    private var range: Range<Int> = Range(30, 30)

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CaptureActivity.cameraDevice = cameraDevice
            startPreview()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CaptureActivity.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            val errorMap = mapOf(
                Pair(ERROR_CAMERA_DEVICE, "Camera Device Error"),
                Pair(ERROR_CAMERA_DISABLED, "Camera Disabled"),
                Pair(ERROR_CAMERA_IN_USE, "Camera In Use"),
                Pair(ERROR_MAX_CAMERAS_IN_USE, "Max Cameras In Use"),
                Pair(ERROR_CAMERA_SERVICE, "Camera Services Error")
            )

            Log.e("Camera Error", errorMap[error] ?: "")
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CaptureActivity.cameraDevice = null
            finish()
        }
    }

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(p0: SurfaceHolder?, format: Int, width: Int, height: Int) {
        }

        override fun surfaceDestroyed(p0: SurfaceHolder?) {
        }

        override fun surfaceCreated(p0: SurfaceHolder?) {
            openCamera()
        }
    }

    private var imageReader: ImageReader? = null

    private var outputStream: FileOutputStream? = null

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.surface_capture_view)
        initViewWithPermissionCheck()
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun initView() {
        captureBtn.setOnClickListener {
            if (isRecordingVideo) stopVideoRecording() else startVideoRecording()
        }

    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        val isSurfaceValid = captureSurfaceView.holder.surface.isValid
        if (isSurfaceValid) {
            openCamera()
        } else {
            captureSurfaceView.holder.addCallback(surfaceCallback)
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera Background")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper ?: error("Background Handler Error"))
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e("CameraBackground", e.toString())
        }
    }


    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)

            val cameraId = manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == cameraFacing
            } ?: manager.cameraIdList[0]
            val cameraCharacteristics = manager.getCameraCharacteristics(cameraId)
            val configMap =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: throw RuntimeException("Cannot get video and preview size")
            sensorOrientation =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: -1
            range =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    ?.lastOrNull() ?: range

            videoSize = chooseVideoSize(configMap.getOutputSizes(ImageReader::class.java))
            previewSize = chooseOptimalSize(
                configMap.getOutputSizes(SurfaceTexture::class.java),
                videoSize.width, videoSize.height, videoSize
            )

            imageReader =
                ImageReader.newInstance(
                    videoSize.width,
                    videoSize.height,
                    ImageFormat.YUV_420_888,
                    1
                )
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Toast.makeText(this, "Cannot access Camera", Toast.LENGTH_SHORT).show()
        } catch (e: NullPointerException) {
            Toast.makeText(this, "Doesn't support Camera2API", Toast.LENGTH_SHORT).show()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            closePreviewSession()
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun startPreview() {
        val cameraDevice = this.cameraDevice
        if (cameraDevice == null || !captureSurfaceView.holder.surface.isValid)
            return

        try {
            closePreviewSession()
            val previewSurface = captureSurfaceView.holder.surface
            previewRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(previewSurface)

            cameraDevice.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(this@CaptureActivity, "Preview Failed", Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        updatePreview()
                    }

                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e("Camera Preview", e.toString())
        }
    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder?.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
    }

    private fun updatePreview() {
        if (cameraDevice == null) return

        try {
            setUpCaptureRequestBuilder(previewRequestBuilder)
            captureSession?.setRepeatingRequest(
                previewRequestBuilder.build(),
                null,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e("Camera Preview Update", e.toString())
        }
    }

    private fun closePreviewSession() {
        captureSession?.close()
        captureSession = null
    }

    private fun setUpImageReader() {
        outputStream = obtainFile().outputStream()
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (isRecordingVideo) {
                val bytes = obtainImageBytes(reader.width, reader.height, image.planes)
                writeToFile(bytes)
            }
            image.close()
        }, backgroundHandler)
    }

    private fun obtainImageBytes(width: Int, height: Int, planes: Array<Image.Plane>): ByteArray {
        val bytes = ByteArray(width * height * 3 / 2)
        var count = 0
        planes.forEach {
            if (isRecordingVideo) {
                val buffer = it.buffer
                val pixelStride = it.pixelStride
                for (i in 0 until buffer.remaining() step pixelStride) {
                    bytes[count++] = buffer.get(i)
                }
            }
        }
        return bytes
    }

    @WorkerThread
    private fun writeToFile(bytes: ByteArray) = GlobalScope.launch(Dispatchers.IO) {
        if (isRecordingVideo) {
            outputStream?.write(bytes)
        }
    }

    private fun startVideoRecording() {
        val cameraDevice = this.cameraDevice
        if (cameraDevice == null || captureSurfaceView.holder.surface.isValid.not()) return

        try {
            closePreviewSession()
            setUpImageReader()
            val previewSurface = captureSurfaceView.holder.surface
            val recordSurface = imageReader?.surface ?: error("Cannot start image reader")
            val surfaces = listOf(previewSurface, recordSurface)

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                .apply {
                    addTarget(previewSurface)
                    addTarget(recordSurface)
                    set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range)
                }

            cameraDevice.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                        Toast.makeText(this@CaptureActivity, "Record Failed", Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        updatePreview()
                        GlobalScope.launch(Dispatchers.Main) {
                            captureBtn.text = "Stop"
                            isRecordingVideo = true
                        }
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e("Camera Recording", e.toString())
        }
    }

    private fun stopVideoRecording() {
        isRecordingVideo = false
        outputStream = null
        captureBtn.text = "Capture"
        startPreview()
    }

    /**
     * Choose 16:9 and max width
     */
    private fun chooseVideoSize(choices: Array<Size>): Size {
        return choices.filter {
            it.width == it.height * 16 / 9 && it.width * it.height <= 1920 * 1080
        }.maxBy { it.width } ?: choices.last()
    }

    /**
     * Choose the smallest size meets the requirement in [choices], which is:
     * 1. Has the require [aspectRatio]
     * 2. Width and Height large than the smallest [width] and [height]
     *
     * If there is no such [Size], then return the arbitrary one
     *
     * @param choices       The list of size the camera supports.
     * @param width         The minimum width desired.
     * @param height        The minimum height desired.
     * @param aspectRatio   The aspect ratio.
     */
    private fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size {
        // Collect the big enough Size
        val w = aspectRatio.width
        val h = aspectRatio.height
        val bigEnough = choices.filter {
            it.width == it.height * w / h && it.width >= width && it.height >= height
        }

        // Pick the smallest Size in bigEnough
        return if (bigEnough.isNotEmpty()) {
            Collections.min(bigEnough) { lhs, rhs ->
                signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
            }
        } else {
            choices[0]
        }
    }

    private fun obtainFile(): File {
        return File(getExternalFilesDir(null), "yuv420_888.yuv")
    }
}