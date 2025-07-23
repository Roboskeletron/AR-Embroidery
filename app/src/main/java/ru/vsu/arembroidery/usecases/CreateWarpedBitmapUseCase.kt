package ru.vsu.arembroidery.usecases

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import ru.vsu.arembroidery.data.MatrixRepository

class CreateWarpedBitmapUseCase(
    private val matrixRepository: MatrixRepository,
    private val transformLandMarkUseCase: TransformLandMarkUseCase
) {
    operator fun invoke(
        pose: Pose,
        mappingMatrix: Matrix,
        previewWidth: Int,
        previewHeight: Int,
        scale: Double,
        offsetX: Double,
        offsetY: Double
    ): Bitmap? {
        val embroideryMat = matrixRepository.getEmbroideryMat()
        if (pose.allPoseLandmarks.isEmpty() || embroideryMat == null) return null

        val leftShoulder = transformLandMarkUseCase(pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)!!, mappingMatrix)
        val rightShoulder = transformLandMarkUseCase(pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)!!, mappingMatrix)
        val leftHip = transformLandMarkUseCase(pose.getPoseLandmark(PoseLandmark.LEFT_HIP)!!, mappingMatrix)
        val rightHip = transformLandMarkUseCase(pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)!!, mappingMatrix)

        val dstPoints = matrixRepository.getDstPoints().apply {
            put(0, 0, leftShoulder.x.toDouble(), leftShoulder.y.toDouble())
            put(1, 0, rightShoulder.x.toDouble(), rightShoulder.y.toDouble())
            put(2, 0, leftHip.x.toDouble(), leftHip.y.toDouble())
            put(3, 0, rightHip.x.toDouble(), rightHip.y.toDouble())
        }
        val srcPoints = matrixRepository.getSrcPoints()

        val t1 = matrixRepository.getT1(previewWidth, previewHeight)
        val mS = matrixRepository.getTS(scale)
        val t2 = matrixRepository.getT2(previewWidth, previewHeight)
        val tOffset = matrixRepository.getTOffset(offsetX, offsetY)

        val mPersp = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val tmp1 = matrixRepository.getTemps()[0]
        val tmp2 = matrixRepository.getTemps()[1]
        val tmp3 = matrixRepository.getTemps()[2]
        val tmp4 = matrixRepository.getTemps()[3]
        val delta = matrixRepository.getDelta()

        // Matrix multiplications
        Core.gemm(mPersp, t2, 1.0, delta, 0.0, tmp1)
        Core.gemm(tmp1, mS, 1.0, delta, 0.0, tmp2)
        Core.gemm(tmp2, t1, 1.0, delta, 0.0, tmp3)
        Core.gemm(tOffset, tmp3, 1.0, delta, 0.0, tmp4)

        // Warp the embroidery image and create bitmap
        return matrixRepository.getWarpedEmbroideryMat()?.let { warpedEmbroideryMat ->
            Imgproc.warpPerspective(
                embroideryMat,
                warpedEmbroideryMat,
                tmp4,
                Size(previewWidth.toDouble(), previewHeight.toDouble()),
                Imgproc.INTER_LINEAR
            )
            createBitmap(
                warpedEmbroideryMat.cols(),
                warpedEmbroideryMat.rows()
            ).apply {
                Utils.matToBitmap(warpedEmbroideryMat, this)
            }
        }
    }
}
