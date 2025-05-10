package ru.vsu.arembroidery.domain

import android.graphics.Matrix
import com.google.mlkit.vision.pose.Pose

data class PoseAnalysisResult(
    val pose: Pose,
    val mappingMatrix: Matrix,
    val imageWith: Int,
    val imageHeight: Int
)
