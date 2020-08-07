package com.example.cameraxtry

import com.google.gson.annotations.SerializedName

class PersonFaceData(name: String, vector: FloatArray) {
    @SerializedName("name")
    val name: String? = null
    @SerializedName("vector")
    val vector: FloatArray? = null
}