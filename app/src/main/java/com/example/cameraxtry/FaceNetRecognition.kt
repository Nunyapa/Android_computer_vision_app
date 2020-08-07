package com.example.cameraxtry

import android.content.Context
import android.util.JsonReader
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.AsynchronousFileChannel.open
import java.nio.channels.FileChannel.open
import kotlin.math.pow
import kotlin.math.sqrt

class FaceNetRecognizer(context: Context){
    private val TRAINED_PATH = "trained_v3.json"
    private val MODEL_PATH = "facenet_int8_quant.tflite"
    var interpreter: Interpreter? = null
    var currentPossibleClasses: List<PersonFaceData>? = null

    init {
        currentPossibleClasses = getAllDataFromJson(context)
        val assets = context.assets

        val remoteModel = FirebaseCustomRemoteModel.Builder("FaceNet_tflite").build()
        FirebaseModelManager.getInstance().getLatestModelFile(remoteModel)
            .addOnCompleteListener { task ->
                val modelFile = task.result
                if (modelFile != null) {
                    interpreter = Interpreter(modelFile)
                } else {
                    val model = assets.open(MODEL_PATH).readBytes()
                    val buffer = ByteBuffer.allocateDirect(model.size).order(ByteOrder.nativeOrder())
                    buffer.put(model)
                    interpreter = Interpreter(buffer)
                }
            }
    }

    private fun runModel(input: Any): Array<FloatArray> {
        //input is already cropped and rescaled Buffer of face. In here i have to return vector
        val output = Array(1) {FloatArray(128)}
        interpreter?.run(input, output)
        return output
    }

    private fun distanceFunction(outputVector: FloatArray, toCompareVector: FloatArray ): Float{
        var dotProduct = 0.0f
        var mag1 = 0.0f
        var mag2 = 0.0f
        for( i in outputVector.indices ) {
            dotProduct += ( outputVector[i] * toCompareVector[i] )
            mag1 += outputVector[i].toDouble().pow(2.0).toFloat()
            mag2 += toCompareVector[i].toDouble().pow(2.0).toFloat()
        }
        mag1 = sqrt( mag1 )
        mag2 = sqrt( mag2 )
        return dotProduct / ( mag1 * mag2 )
    }

    fun run(input: Any, minTreshold: Float = 0.5F): PredictedPerson{
        val outputVector = runModel(input)[0]
        var maximalScore = 0f
        var labelForMaximalScore = "Unknown"
        val finalPrediction = PredictedPerson(labelForMaximalScore, maximalScore)

        for (person in currentPossibleClasses!!){
            val curScore = distanceFunction(outputVector, person.vector!!)
            if (curScore >= minTreshold){
                maximalScore = curScore
                labelForMaximalScore = person.name.toString()
            }
        }
        finalPrediction.apply {
            name = labelForMaximalScore
            score = maximalScore
        }
        return finalPrediction
    }

    fun getJsonFileFromAsset(context: Context, fileName: String): String? {
        val jsonString: String
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return jsonString
    }

    fun getAllDataFromJson(applicationContext: Context): List<PersonFaceData> {
        val jsonFileString = getJsonFileFromAsset(applicationContext, TRAINED_PATH)
        val listPersonType = object : TypeToken<List<PersonFaceData>>() {}.type
        val classes: List<PersonFaceData> = Gson().fromJson(jsonFileString, listPersonType)
        return classes
    }
}