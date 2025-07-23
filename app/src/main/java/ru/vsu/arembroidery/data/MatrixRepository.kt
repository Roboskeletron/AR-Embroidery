package ru.vsu.arembroidery.data

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

class MatrixRepository {
    private val dstPointMat = Mat(4, 1, CvType.CV_32FC2)
    private val srcPointMat = Mat(4, 1, CvType.CV_32FC2)
    private var embroideryMat : Mat? = null
    private var warpedEmbroideryMat : Mat? = null
    private val t1 = Mat.eye(3, 3, CvType.CV_64F)
    private val t2 = Mat.eye(3, 3, CvType.CV_64F)
    private val mS = Mat.eye(3, 3, CvType.CV_64F)
    private val tOffset = Mat.eye(3, 3, CvType.CV_64F)
    private val temps = listOf<Mat>(Mat(), Mat(), Mat(), Mat())
    private val delta = Mat()

    fun getT1(width: Int, height: Int) : Mat = t1.apply {
        put(0, 2, -width / 2.0)
        put(1, 2, -height / 2.0)
        put(0, 0, 1.0)
        put(1, 1, 1.0)
    }

    fun getT2(width: Int, height: Int) : Mat = t2.apply {
        put(0, 2, width / 2.0)
        put(1, 2, height / 2.0)
        put(0, 0, 1.0)
        put(1, 1, 1.0)
    }

    fun getTS(scale: Double) : Mat = mS.apply {
        put(0, 2, 0.0)
        put(1, 2, 0.0)
        put(0, 0, scale)
        put(1, 1, scale)
    }

    fun getTOffset(x: Double, y: Double) : Mat = tOffset.apply {
        put(0, 2, x)
        put(1, 2, y)
        put(0, 0, 1.0)
        put(1, 1, 1.0)
    }

    fun getTemps() = temps

    fun getDelta() = delta

    fun getDstPoints() = dstPointMat

    fun getSrcPoints() = srcPointMat

    fun getEmbroideryMat() = embroideryMat

    fun getWarpedEmbroideryMat() = warpedEmbroideryMat

    fun updateWarpedEmbroideryMat(width: Int, height: Int) {
        warpedEmbroideryMat?.release()
        if (width <= 0)  {
            warpedEmbroideryMat = null
            return
        }

        warpedEmbroideryMat = Mat(
            height,
            width,
            CvType.CV_8UC4,
            Scalar(0.0, 0.0, 0.0, 0.0)
        )
    }

    fun updateEmbroideryMat(mat: Mat?) {
        embroideryMat?.release()
        embroideryMat = mat?.apply {
            srcPointMat.apply {
                put(0, 0, 0.0, 0.0)
                put(1, 0, mat.width().toDouble(), 0.0)
                put(2, 0, 0.0, mat.height().toDouble())
                put(3, 0, mat.width().toDouble(), mat.height().toDouble())
            }
        }
    }
}
