package devdan.simple.camera

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectionProcessor(
    private val onDetect: (faces: List<Face>?) -> Unit
) : ImageAnalysis.Analyzer {
    private val _detector: FaceDetector

    init {
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        _detector = FaceDetection.getClient(realTimeOpts)
    }

    private fun detectInImage(image: InputImage): Task<List<Face>> {
        return _detector.process(image)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        mediaImage?.let {
            val image = InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)
            _detector.process(image)
                .addOnSuccessListener { results ->
                    onDetect(results)
                }
                .addOnFailureListener {
                    onDetect(null)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}