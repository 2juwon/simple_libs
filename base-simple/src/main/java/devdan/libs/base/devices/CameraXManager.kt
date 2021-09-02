package devdan.libs.base.devices

import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.util.DisplayMetrics
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import devdan.libs.base.utils.ImageUtil
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraManager(val context: Context) {
    /** androidX.camera */
    private var _lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private var _preview: Preview? = null
    private var _imageCapture: ImageCapture? = null
    private var _camera: Camera? = null
    private var _cameraProvider: ProcessCameraProvider? = null
    private var _viewFinder: PreviewView? = null
    private var _displayListener: DisplayManager.DisplayListener? = null

    private lateinit var _cameraExecutor: ExecutorService

    var displayId: Int = -9999

    private val displayManager by lazy {
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    fun setViewFinder(viewFinder: PreviewView) {
        this._viewFinder = viewFinder
    }

    fun getViewFinder(): PreviewView? = this._viewFinder

    fun setDisplayListener(displayListener: DisplayManager.DisplayListener) {
        this._displayListener = displayListener
        displayManager.registerDisplayListener(displayListener, null)
    }

    @Throws(Exception::class)
    fun setUpCamera(lifecycleOwner: LifecycleOwner, handleError: ((String) -> Unit)? = null) {
        _cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            try {
                _cameraProvider = cameraProviderFuture.get()
                bindPreview(lifecycleOwner)
            } catch (e: Exception) {
                handleError?.let {
                    e.message?.let { message -> it(message) }
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(takePhotoListener: TakePhotoListener) {
        _imageCapture?.let { imageCapture ->
            val photoFile =
                File(ImageUtil.getOutputDirectory(context), ImageUtil.getImageFileName())
            val metadata = ImageCapture.Metadata().apply {
                isReversedHorizontal = _lensFacing == CameraSelector.LENS_FACING_FRONT
            }
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        devdan.libs.base.utils.Logger.e(
                            "Camera",
                            "Photo capture failed: ${exc.message}",
                            exc
                        )
                        takePhotoListener.onError()
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        savedUri.path?.let { takePhotoListener.onTakePhoto(it) }
                    }
                }
            )
        }
    }

    fun setTargetRotation(rotation: Int) {
        _imageCapture?.targetRotation = rotation
    }

    fun shutdown() {
        displayId = -9999
        this._viewFinder = null

        if (::_cameraExecutor.isInitialized) {
            // Shut down our background executor
            _cameraExecutor.shutdown()
        }

        // Unregister the broadcast receivers and listeners
        if (_displayListener != null) {
            try {
                displayManager.unregisterDisplayListener(_displayListener)
            } catch (e: IllegalArgumentException) {
            }
        }
    }

    @Throws(Exception::class)
    private fun bindPreview(lifecycleOwner: LifecycleOwner) {
        val metrics = DisplayMetrics().also { _viewFinder!!.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = _viewFinder!!.display.rotation

        val cameraProvider = _cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(_lensFacing).build()


        _preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        _imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
        cameraProvider.unbindAll()

        _camera =
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                _preview,
                _imageCapture
            )

        // Attach the viewfinder's surface provider to preview use case
//        preview?.setSurfaceProvider(viewFinder!!.createSurfaceProvider())
        _preview?.setSurfaceProvider(_viewFinder!!.surfaceProvider)
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun hasFrontCamera(): Boolean {
        return _cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}

interface TakePhotoListener {
    fun onTakePhoto(path: String)
    fun onError()
}