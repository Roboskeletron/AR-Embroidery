package ru.vsu.arembroidery

import android.graphics.PointF
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var arSceneView: ARSceneView
    private lateinit var poseDetector: PoseDetector

    private val semaphore = Semaphore(1, 0)

    private lateinit var lastFrame : Frame
    private lateinit var modelNode: ModelNode
    private var anchorNode: AnchorNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var view = View.inflate(this, R.layout.activity_main, null)

        setContentView(view)

        poseDetector = PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )

        arSceneView = view.findViewById<ARSceneView>(R.id.arSceneView)

        arSceneView.apply {
            lifecycle = this@MainActivity.lifecycle
            planeRenderer.isEnabled = true
            configureSession { session, config ->
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                config.depthMode = Config.DepthMode.DISABLED
            }
            onTrackingFailureChanged = { reason ->
                Log.d(TAG, "Tracking failure reason: $reason")
            }
            onSessionUpdated = { _, frame -> updateFrame(frame) }
        }

        lifecycleScope.launch {
            modelNode = buildModelNode()!!
        }
    }

    private fun updateFrame(frame: Frame) {
        lastFrame = frame
        if (frame.camera.trackingState != TrackingState.TRACKING) return
        if (!semaphore.tryAcquire()) return

        val cameraImage = frame.tryAcquireCameraImage()

        if (cameraImage == null) {
            Log.d(TAG, "Camera image is null")
            semaphore.release()
            return
        }

        Log.d(TAG, "Camera image acquired, launching pose detection")

//    val cameraId = session.cameraConfig.cameraId ?: return
        val imageRotation = 90 // displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
        Log.d(TAG, "Image rotation: $imageRotation")
        val inputImage = InputImage.fromMediaImage(cameraImage, imageRotation)

        poseDetector.process(inputImage)
            .addOnSuccessListener { detectedPose ->
                Log.d(TAG, "Pose detected successfully")
                onDetectedPose(detectedPose)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Pose detection failed", e)
            }
            .addOnCompleteListener {
                cameraImage.close()
                semaphore.release()
            }
    }

    suspend fun buildModelNode(): ModelNode? {
        arSceneView.modelLoader.loadModelInstance(
            "https://sceneview.github.io/assets/models/DamagedHelmet.glb"
        )?.let { modelInstance ->
            return ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 0.5f,
            ).apply {
                isEditable = true
            }
        }
        return null
    }

    private fun onDetectedPose(
        pose: com.google.mlkit.vision.pose.Pose
    ) {
        val leftShoulder =
            pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER)
        val rightShoulder =
            pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP)

        Log.v(TAG, "Left shoulder: (${leftShoulder?.position?.x},${leftShoulder?.position?.y})," +
                "right shoulder: (${rightShoulder?.position?.x},${rightShoulder?.position?.y})," +
                "left hip: (${leftHip?.position?.x},${leftHip?.position?.y})," +
                "right hip: (${rightHip?.position?.x},${rightHip?.position?.y})")
        var anchorPose = tryGetAnchorPose(leftShoulder, rightHip)
        if (anchorPose == null) {
            anchorPose = tryGetAnchorPose(rightShoulder, leftHip)
        }

        anchorPose?.let {
            anchorNode?.detachAnchor()
            anchorNode = addAnchorNode(arSceneView.session!!.createAnchor(anchorPose))
            Log.d(TAG, "Model should be added")
        }
//    session.allAnchors.forEach { it.detach() }
    }

    private fun addAnchorNode(anchor: Anchor) : AnchorNode {
        var node = AnchorNode(arSceneView.engine, anchor)
            .apply {
                isEditable = true
                addChildNode(modelNode)
            }
        arSceneView.addChildNode(node)

        return node
    }

    private fun tryGetAnchorPose(
        shoulder: com.google.mlkit.vision.pose.PoseLandmark?,
        hip: com.google.mlkit.vision.pose.PoseLandmark?
    ): Pose? {
        if (shoulder == null || hip == null) return null
        val shoulderHit = getHitResult(PointF(shoulder.position.x, shoulder.position.y))
        val hipHit = getHitResult(PointF(hip.position.x, hip.position.y))
        if (shoulderHit == null || hipHit == null) return null
        return shoulderHit.hitPose
//        return Pose.makeInterpolated(shoulderHit.hitPose, hipHit.hitPose, 0.5f)
    }

    private val convertFloats = FloatArray(4)
    private val convertFloatsOut = FloatArray(4)

    private fun getHitResult(point: PointF): HitResult? {
        convertFloats[0] = point.x
        convertFloats[1] = point.y
        lastFrame.transformCoordinates2d(
            Coordinates2d.IMAGE_PIXELS,
            convertFloats,
            Coordinates2d.VIEW,
            convertFloatsOut
        )

        return lastFrame.hitTestInstantPlacement(convertFloatsOut[0], convertFloatsOut[1], 5f).firstOrNull()
    }

    private fun Frame.tryAcquireCameraImage(): Image? = try {
        acquireCameraImage()
    } catch (e: NotYetAvailableException) {
        null
    } catch (e: Throwable) {
        throw e
    }
}