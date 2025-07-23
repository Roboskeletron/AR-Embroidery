package ru.vsu.arembroidery.usecases

import android.graphics.Matrix
import android.graphics.PointF
import com.google.mlkit.vision.pose.PoseLandmark

class TransformLandMarkUseCase {
    operator fun invoke(landmark: PoseLandmark, mappingMatrix: Matrix): PointF {
        val points = floatArrayOf(landmark.position.x, landmark.position.y)
        mappingMatrix.mapPoints(points)
        return PointF(points[0], points[1])
    }
}