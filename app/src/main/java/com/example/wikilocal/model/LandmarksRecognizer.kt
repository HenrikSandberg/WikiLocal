package com.example.wikilocal.model

import android.graphics.Bitmap
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage

class LandmarksRecognizer {
    private val options = FirebaseVisionCloudDetectorOptions.Builder()
        .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
        .setMaxResults(15)
        .build()

    fun recognizeImage(bitmap: Bitmap): List<Double> {
        val detector = FirebaseVision.getInstance().getVisionCloudLandmarkDetector(options)
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        var mostConfidants:Float? = null

        var returnList:List<Double>? = null

        with(detector.detectInImage(image)) {
            addOnSuccessListener { fireLandmarks->
                for (landmark in fireLandmarks) {
                    val confidence = landmark.confidence

                    if (mostConfidants == null ) {
                        mostConfidants = confidence
                    }

                    if (confidence > mostConfidants!!){
                        for (loc in landmark.locations) {
                            returnList = listOf(loc.latitude, loc.longitude)
                        }
                    }
                }
            }
            addOnFailureListener {
                println(it)
            }
        }
        return returnList ?: listOf(0.0, 0.0)
    }
}