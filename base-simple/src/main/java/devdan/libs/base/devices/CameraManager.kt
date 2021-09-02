package devdan.libs.base.devices

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ExifInterface
import android.media.ImageReader
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import devdan.libs.base.utils.ImageUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class CameraManager2(private val activity: Activity) {
    companion object {
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS =
            arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")
        private val ORIENTATIONS = SparseIntArray().apply {
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
        }
    }

    private lateinit var cameraId: String
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private lateinit var captureRequest: CaptureRequest
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private val mCameraOpenCloseLock: Semaphore = Semaphore(1)

    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread
    lateinit var textureView: TextureView

    private var imageReader: ImageReader? = null
    private var previewSize: Size? = null
    private var map: StreamConfigurationMap? = null

    fun start() {
        startBackgroundThread()
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = textureListener
        }
    }

    fun stop() {
        try {
            closeCamera()
            stopBackgroundThread()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera Background")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            cameraDevice?.close()
            cameraDevice = null

//            if (null != mImageReader) {
//                mImageReader.close()
//                mImageReader = null
//            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    @Throws(CameraAccessException::class)
    fun openCamera(width: Int, height: Int) {

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setUpCameraOutputs(width, height)
            configureTransform(width, height)

            val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        } else {
            ActivityCompat.requestPermissions(
                activity,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    continue
                }

                val map = (characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ) ?: continue)

                this.map = map

                // For still image captures, we use the largest available size.
                val largest: Size = Collections.max(
                    map.getOutputSizes(ImageFormat.JPEG).asList(),
                    CompareSizesByArea()
                )

                imageReader = ImageReader.newInstance(
                    largest.width, largest.height,
                    ImageFormat.JPEG,  /*maxImages*/2
                ).apply {
                    setOnImageAvailableListener(
                        null, backgroundHandler
                    )
                }
                val displaySize = Point()
                activity.windowManager.defaultDisplay.getSize(displaySize)
                var maxPreviewWidth = displaySize.x
                var maxPreviewHeight = displaySize.y
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH
                }
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT
                }

                // Danger! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                previewSize = chooseOptimalSize(
                    map.getOutputSizes(SurfaceTexture::class.java),
                    width, height, maxPreviewWidth,
                    maxPreviewHeight, largest
                )
                this.cameraId = cameraId
                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Toast.makeText(
                activity,
                "Camera2 API not supported on this device",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Throws(CameraAccessException::class)
    private fun createCameraPreview() {
//        if (cameraDevice == null || !textureView.isAvailable || imageDimensions == null) {
//            return
//        }

        val texture: SurfaceTexture? = textureView.surfaceTexture!!
        texture!!.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)

        val surface = Surface(texture)

        captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        cameraDevice!!.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) {
                        return
                    }
                    cameraCaptureSession = session
                    try {
                        updatePreview()
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(activity, "카메라 설정이 실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_LONG)
                        .show()
                }
            },
            null
        )
    }

    @Throws(CameraAccessException::class)
    private fun updatePreview() {
        if (cameraDevice == null) {
            return
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        cameraCaptureSession?.setRepeatingRequest(
            captureRequestBuilder.build(),
            null,
            backgroundHandler
        )
    }

    @Throws(InterruptedException::class)
    fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        backgroundThread.join()
    }

    private fun chooseOptimalSize(
        choices: Array<Size>, textureViewWidth: Int,
        textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size
    ): Size? {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = ArrayList()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough: MutableList<Size> = ArrayList()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight && option.height == option.width * h / w) {
                if (option.width >= textureViewWidth &&
                    option.height >= textureViewHeight
                ) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        return when {
            bigEnough.size > 0 -> {
                Collections.min(bigEnough, CompareSizesByArea())
            }
            notBigEnough.size > 0 -> {
                Collections.max(notBigEnough, CompareSizesByArea())
            }
            else -> {
                Log.e("Camera2", "Couldn't find any suitable preview size")
                choices[0]
            }
        }
    }

    private fun configureTransform(width: Int, height: Int) {
        if (previewSize == null) {
            return
        }
        //단말기 방향이 방향이 변경되어있는지 확인
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val bufferRect = RectF(
            0f, 0f, previewSize!!.height.toFloat(),
            previewSize!!.width.toFloat()
        )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale: Float =
                (height.toFloat() / previewSize!!.height).coerceAtLeast(width.toFloat() / previewSize!!.width)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    fun takePhoto(listener: PhotoCaptureListener) {
        var jpegSizes: Array<Size> = arrayOf()
        if (map != null) jpegSizes = map?.getOutputSizes(ImageFormat.JPEG)!!
        var width = 640
        var height = 480
        if (jpegSizes.isNotEmpty()) {
            val min = Collections.min(jpegSizes.asList(), CompareSizesByArea())
            width = min.width
            height = min.height
        }
        val imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
        val outputSurfaces = arrayListOf<Surface>().apply {
            add(imageReader.surface)
            add(Surface(textureView.surfaceTexture))
        }

        val captureBuilder =
            cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)!!
        captureBuilder.addTarget(imageReader.surface)
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

        val rotation = activity.windowManager.defaultDisplay.rotation
        captureBuilder.set(
            CaptureRequest.JPEG_ORIENTATION,
            ORIENTATIONS.get(rotation)
        )

        val readerListener = object : ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(reader: ImageReader?) {

                try {
                    val image = imageReader.acquireLatestImage()
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)
                    save(bytes)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            @Throws(IOException::class)
            private fun save(bytes: ByteArray) {
                val fileName = ImageUtil.getImageFileName()
                val file = File(
                    ImageUtil.getOutputPhotoDir(activity),
                    fileName
                )
                file.createNewFile()
                var output: FileOutputStream? = null
                try {
                    output = FileOutputStream(file)
                    output.write(bytes)

                    val exif = ExifInterface(file.path)
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, "8")
                    exif.saveAttributes()
                    listener.onPhotoCaptured(fileName, file.path)
                } finally {
                    output?.close()
                }
            }
        }

        val thread = HandlerThread("Camera Picture")
        thread.start()
        val backgroundHandler = Handler(thread.looper)
        imageReader.setOnImageAvailableListener(readerListener, backgroundHandler)
        val captureCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
                createCameraPreview()
            }
        }

        cameraDevice?.createCaptureSession(outputSurfaces, object :
            CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                try {
                    session.capture(captureBuilder.build(), captureCallback, backgroundHandler)
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {

            }

        }, backgroundHandler)
    }

    private val textureListener: TextureView.SurfaceTextureListener = object :
        TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            try {
                openCamera(width, height)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private val stateCallback: CameraDevice.StateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                mCameraOpenCloseLock.release()
                cameraDevice = camera
                try {
                    createCameraPreview()
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }

            override fun onDisconnected(camera: CameraDevice) {
                mCameraOpenCloseLock.release()
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                mCameraOpenCloseLock.release()
                camera.close()
                cameraDevice = null
            }
        }

    interface PhotoCaptureListener {
        fun onPhotoCaptured(fileName: String, filePath: String)
    }
}

class CompareSizesByArea : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size): Int {
        return java.lang.Long.signum(
            lhs.width.toLong() * lhs.height -
                    rhs.width.toLong() * rhs.height
        )
    }
}