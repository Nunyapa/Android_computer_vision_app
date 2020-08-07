package com.example.cameraxtry

import android.R.attr.bitmap
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.SurfaceHolder
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread


class FaceDetectionAnalyzer(context: Context, surfaceHolder: SurfaceHolder): ImageAnalysis.Analyzer {
    private var realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
        .build()
    // Configure the FirebaseVisionFaceDetector
    private var detector = FirebaseVision.getInstance().getVisionFaceDetector(realTimeOpts)
    private var isProcessing = AtomicBoolean(false)
    private val model = FaceNetRecognizer( context )
    val rectDrawer : RectangleDrawer = RectangleDrawer(surfaceHolder)

    // Used to determine whether the incoming frame should be dropped or processed.
    private fun degreesToFirebaseRotation(degrees: Int): Int = when(degrees) {
        0 -> FirebaseVisionImageMetadata.ROTATION_0
        90 -> FirebaseVisionImageMetadata.ROTATION_90
        180 -> FirebaseVisionImageMetadata.ROTATION_180
        270 -> FirebaseVisionImageMetadata.ROTATION_270
        else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {
        // android.media.Image -> android.graphics.Bitmap
        val mediaImage = image.image
        val imageRotation = image.imageInfo.rotationDegrees
        val rotation = degreesToFirebaseRotation(imageRotation)
        // If the previous frame is still being processed, then skip this frame
        if (!isProcessing.get()) {
            isProcessing.set(true)
            val inputImage = FirebaseVisionImage.fromMediaImage(mediaImage!!, rotation)
            detector.detectInImage(inputImage)
                .addOnSuccessListener { faces ->
                    // Start a new thread to avoid frequent lags.
                    thread(priority = 1) {
                        val predictions = ArrayList<PredictedPerson>()
                        for (face in faces) {
                            try {
                                //IMAGE PROCESSING
                                val rect = face.boundingBox
                                val resultBmp = Bitmap.createBitmap(
                                    inputImage.bitmap,
                                    rect.centerX() - rect.width() / 2,
                                    rect.centerY() - rect.height() / 2,
                                    rect.width(),
                                    rect.height()
                                )
                                val scaledBitmap =
                                    Bitmap.createScaledBitmap(resultBmp, 160, 160, false)

                                //MODEL PREDICTION
                                val modelOutput = model.run(convertBitmapToBuffer(scaledBitmap))
                                modelOutput.rect = rect
                                predictions.add(modelOutput)

                            } catch (e: Exception) {
                                // If any exception occurs if this box and continue with the next boxes.
                                continue
                            }
                        }
                        // Declare that the processing has been finished and the system is ready for the next frame.

                        rectDrawer.draw(predictions)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Error", e.message.toString())
                }
            isProcessing.set(false)
        }
    }

    private fun convertBitmapToBuffer( image : Bitmap) : ByteBuffer {
//        var a = image.rowBytes
//        val x: Int = image.getWidth()
//        val y: Int = image.getHeight()
//        val intArray = IntArray(x * y)
//
//        image.getPixels(intArray, 0, x, 0, 0, x, y)
        val imgSize = 160
        val imageByteBuffer = ByteBuffer.allocateDirect( 1 * imgSize * imgSize * 3 * 4)
        imageByteBuffer.order( ByteOrder.nativeOrder() )
        for (x in 0 until imgSize) {
            for (y in 0 until imgSize) {
                val pixelValue = image.getPixel( x , y )
                imageByteBuffer.putFloat((((pixelValue shr 16 and 0xFF) - 128f) / 128f))
                imageByteBuffer.putFloat((((pixelValue shr 8 and 0xFF) - 128f) / 128f ))
                imageByteBuffer.putFloat((((pixelValue and 0xFF) - 128f )/ 128f))
            }
        }
        return imageByteBuffer
    }

}
