package com.example.cameraxtry

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.Log
import android.view.SurfaceHolder
import com.google.firebase.ml.vision.face.FirebaseVisionFace


class RectangleDrawer(surfaceHolder: SurfaceHolder){
    var _sh = surfaceHolder
    var paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 10f
    }

    public fun draw(predictedPersons: ArrayList<PredictedPerson>) {
        val canvas = _sh.lockCanvas()
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)

        val canvasHeight = canvas.height
        val canvasWidth = canvas.width
        val heightScale = canvasHeight / 480F
        val widthScale = canvasWidth / 360F

        for (person in predictedPersons){
            val bounds = person.rect!!
            canvas.drawRect(
                bounds.left * widthScale,
                bounds.top * heightScale,
                bounds.right * widthScale,
                bounds.bottom * heightScale,
                paint
            )
            Log.i("drawer", person.name.toString())
        }
        _sh.unlockCanvasAndPost(canvas)

    }


}