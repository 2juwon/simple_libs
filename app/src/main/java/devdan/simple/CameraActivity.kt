package devdan.simple

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import devdan.libs.base.extensions.showToast
import devdan.libs.base.utils.ImageUtil
import devdan.libs.base.utils.ImageUtil.getImageFileName
import devdan.libs.base.utils.ImageUtil.getOutputPhotoDir
import devdan.libs.base.utils.Logger
import devdan.simple.camera.FaceDetectionInImageProcessor
import devdan.simple.camera.FaceDetectionProcessor
import devdan.simple.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

class CameraActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CameraXBasic"
        private val permissions: Array<String> = arrayOf(Manifest.permission.CAMERA)
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val MAX_DETECT_COUNT = 5

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }

    private lateinit var binding: ActivityCameraBinding
    private lateinit var viewFinder: PreviewView
    private val cameraExecutor: Executor by lazy { ContextCompat.getMainExecutor(this) }

    private var preview: Preview? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var isTakingPicture: Boolean = false
    var isFindingPicture: Boolean = false
        private set
    var isFindCompletePicture: Boolean = false
        private set
    private var detectCount: Int = 0
    private var detectImages: ArrayList<String> = arrayListOf()
    private val _isFaceDetect: MutableLiveData<Boolean> = MutableLiveData(false)
    val isFaceDetect: LiveData<Boolean>
        get() = _isFaceDetect

    private val displayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.containsValue(false)) {
                showToast("permission denied")
                finish()
            } else {
                init()
            }
        }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = binding.root.let { view ->
            if (displayId == view.display?.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissions()) {
                init()
            } else {
                requestPermissionLauncher.launch(permissions)
            }
        } else {
            init()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun init() {
        displayManager.registerDisplayListener(displayListener, null)
        viewFinder = binding.testPreview
        setUpCamera(this)

        binding.buttonTestCapture.setOnClickListener {
            takePhoto()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions(): Boolean =
        !permissions.map { permission: String ->
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }.contains(false)

    private fun setUpCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, cameraExecutor)
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        val metrics: Rect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            val rect = Rect()
            windowManager.defaultDisplay.getRectSize(rect)
            rect
//            val displayMetrics = DisplayMetrics()
//            windowManager.defaultDisplay.getMetrics(displayMetrics)
//            Rect(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
        Log.d(TAG, "Screen metrics: ${metrics.width()} x ${metrics.height()}")

        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation
        Log.d(TAG, "Rotation: $rotation")

        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder().build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            // How the Image Analyser should pipe in input, 1. every frame but drop no frame, or
            // 2. go to the latest frame and may drop some frame. The default is 2.
            // STRATEGY_KEEP_ONLY_LATEST. The following line is optional, kept here for clarity
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    FaceDetectionProcessor() { results ->
                        val isDetect = results?.isNotEmpty()
                        if (isDetect == true) {
                            if (!isTakingPicture && detectCount < MAX_DETECT_COUNT) {
                                detectCount++
                                Logger.e(
                                    TAG,
                                    "detectCount : $detectCount"
                                )
                                takePhoto()
                            }
                        }

                        if (_isFaceDetect.value != isDetect) {
                            _isFaceDetect.value = isDetect
                        }
                    })
            }

        cameraProvider.unbindAll()

        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture,
            imageAnalyzer
        )

        // Attach the viewfinder's surface provider to preview use case
        preview?.setSurfaceProvider(viewFinder.surfaceProvider)
    }

    private fun getFaceImageFile(context: Context) : File = File(getOutputPhotoDir(context), getImageFileName())

    private fun takePhoto() {
        if (camera == null) {
            return
        }

        try {
            imageCapture?.let { imageCapture ->
//                imageCapture.takePicture(
//                    cameraExecutor,
//                    object : ImageCapture.OnImageCapturedCallback() {
//                        override fun onCaptureSuccess(image: ImageProxy) {
//                            super.onCaptureSuccess(image)
//                            Logger.e("ljw", image.toString())
//                            Logger.e(TAG, "capture success : ${image.imageInfo.rotationDegrees}")
//                        }
//
//                        override fun onError(exception: ImageCaptureException) {
//                            super.onError(exception)
//                            Logger.e("ljw", exception.toString())
//                        }
//                    })

                val photoFile: File = getFaceImageFile(this)
                Logger.e(TAG, "photo file: $photoFile")
                val metadata = ImageCapture.Metadata().apply {
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .setMetadata(metadata)
                    .build()
                imageCapture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val savedUri = Uri.fromFile(photoFile)
                            Logger.e(TAG, "saved file : $savedUri")
                            savedUri.path?.let {
                                detectImages.add(it)
                                if (detectImages.size == MAX_DETECT_COUNT) {
                                    findFaceImage()
                                }
                            }
                            isTakingPicture = false
                        }

                        override fun onError(exc: ImageCaptureException) {
                            isTakingPicture = false
                        }
                    }
                )
            }
        } catch (e: Exception) {
        }
    }

    private fun findFaceImage() {
        if (isFindingPicture || isFindCompletePicture) {
            return
        }

        isFindingPicture = true

        val tempImages = ArrayList<String>(detectImages)
        val process = FaceDetectionInImageProcessor(tempImages)
        process.setEvent { results ->
            results
                ?.takeIf { it.isNotEmpty() }
                ?.let { result ->
                    Logger.e(TAG, "results count : ${result.size}")
//                    return@setEvent _faceDetectedImage.postValue(result[0])
                }

//            _faceDetectedImage.postValue("")
            isFindCompletePicture = true
            isFindingPicture = false
        }
        process.execute()
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio =
            kotlin.math.max(width, height).toDouble() / kotlin.math.min(width, height)
        if (kotlin.math.abs(previewRatio - RATIO_4_3_VALUE) <= kotlin.math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }
}