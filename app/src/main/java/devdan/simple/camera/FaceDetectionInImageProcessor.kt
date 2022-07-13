package devdan.simple.camera

import android.graphics.BitmapFactory
import android.media.Image
import android.media.ImageReader
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Task
import com.google.android.odml.image.MediaMlImageBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import devdan.libs.base.utils.ImageUtil.TAG
import devdan.libs.base.utils.Logger
import java.io.IOException

class FaceDetectionInImageProcessor(
    private var images: ArrayList<String>
) : Runnable {

    private val _detector: FaceDetector
    private val _faces: ArrayList<String>
    private var _detectCount: Int = 0
    private var _resultEvent: ((images: List<String>?) -> Unit)? = null
    private var isRunning: Boolean = false

    init {
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        _detector = FaceDetection.getClient(highAccuracyOpts)
        _faces = arrayListOf()
    }

    private fun detectInImage(image: InputImage): Task<List<Face>> {
        return _detector.process(image)
    }

    fun execute() {
        _detectCount = 0
        val thread = Thread(this)
        thread.start()
    }

    fun setEvent(resultEvent: (images: List<String>?) -> Unit) {
        this._resultEvent = resultEvent
    }

    fun stop() {
        isRunning = false
    }

    override fun run() {
        if (images.isEmpty()) {
            _resultEvent?.invoke(_faces)
        } else {
            images.forEach { path ->
                try {
                    BitmapFactory.decodeFile(path).let { bitmap ->
                        if (bitmap == null) {
                            detected()
                        } else {
                            val orientation = getExifOrientation(path)
                            val rotateDegree = getRotateDegree(orientation)
                            Logger.e(Logger.TAG, "image : $path, $rotateDegree")

                            val image = InputImage.fromBitmap(bitmap, rotateDegree)

                            detectInImage(image)
                                .addOnSuccessListener { faces ->
                                    Logger.e(Logger.TAG, "face count : ${faces.size}")
                                    if (faces.isNotEmpty()) {
                                        _faces.add(path)
                                    }
                                    detected()
                                    bitmap.recycle()
                                }
                                .addOnFailureListener { e ->
                                    Logger.e(Logger.TAG, e.message, e)
                                    detected()
                                    bitmap.recycle()
                                }
                        }
                    }
                } catch (e: Exception) {
                    detected()
                }
            }
        }
    }

    private fun detected() {
        _detectCount++

        if (_detectCount == images.size) {
            _resultEvent?.invoke(_faces)
        }
    }

    private fun getExifOrientation(filepath: String): Int {
        var degree = ExifInterface.ORIENTATION_UNDEFINED
        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(filepath)
        } catch (ex: IOException) {
            Logger.e(TAG, ex.message, ex)
        }

        if (exif != null) {
            val orientation: Int = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            if (orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                degree = orientation
            }
        }
        return degree
    }

    private fun getRotateDegree(ori: Int): Int {
        return when (ori) {
            0,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }
}