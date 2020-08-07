package com.example.cameraxtry

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.util.SparseArray
import android.view.SurfaceHolder
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var _sh: SurfaceHolder? = null
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        _sh = surfaceView.holder
        surfaceView.setZOrderOnTop(true)
        _sh?.setFormat(PixelFormat.TRANSPARENT)


        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val rectDrawer = RectangleDrawer(_sh!!)

            // Preview
            preview = Preview.Builder()
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceDetectionAnalyzer(this, _sh!!))
                }

            // Select back camera
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview,  imageAnalyzer)
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

}

//class FaceDetectionAnalyzer(detector: FirebaseVisionFaceDetector, private val listener: FaceListener): ImageAnalysis.Analyzer {
//    var detector: FirebaseVisionFaceDetector? = null
//    init {
//        this.detector = detector
//    }
//
//    private fun degreesToFirebaseRotation(degrees: Int): Int = when(degrees) {
//        0 -> FirebaseVisionImageMetadata.ROTATION_0
//        90 -> FirebaseVisionImageMetadata.ROTATION_90
//        180 -> FirebaseVisionImageMetadata.ROTATION_180
//        270 -> FirebaseVisionImageMetadata.ROTATION_270
//        else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
//    }
//
//    @SuppressLint("UnsafeExperimentalUsageError")
//    override fun analyze(image: ImageProxy) {
//
//        if (detector != null){
//            val mediaImage = image.image
//            val rotationDegrees = image.imageInfo.rotationDegrees
//            val imageRotation = degreesToFirebaseRotation(rotationDegrees)
//            if (mediaImage != null) {
//                val firebaseImage = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
//                // Pass image to an ML Kit Vision API
//                detector!!.detectInImage(firebaseImage)
//                    .addOnSuccessListener { faces ->
//                        // Task completed successfully
//                        listener(faces)
//                    }
//                    .addOnFailureListener { e ->
//                        // Task failed with an exception
//                        Log.d("CameraXBasic", e.message.toString())
//                    }
//            }
//        }
//        image.close()
//    }
//}
