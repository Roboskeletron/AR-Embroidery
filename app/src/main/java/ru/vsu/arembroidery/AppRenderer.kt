package ru.vsu.arembroidery

import android.graphics.PointF
import android.media.Image
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch
import ru.vsu.arembroidery.common.helpers.DisplayRotationHelper

class AppRenderer(
  private val activity: MainActivity,
  private val arSceneView: ARSceneView
) : DefaultLifecycleObserver {

  companion object {
    private const val TAG = "AppRenderer"
  }

  private val displayRotationHelper = DisplayRotationHelper(activity)

  private val poseDetector = PoseDetection.getClient(
    PoseDetectorOptions.Builder()
      .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
      .build()
  )

  private var isDetectionInProcess = false
  private var embroideryModel: io.github.sceneview.model.ModelInstance? = null
  private var anchorNode: AnchorNode? = null

  suspend fun loadResources() {
    embroideryModel = arSceneView.modelLoader.loadModelInstance("models/plane.obj")
    Log.d(TAG, "Embroidery model loaded")
  }

  override fun onResume(owner: LifecycleOwner) {
    displayRotationHelper.onResume()
  }

  override fun onPause(owner: LifecycleOwner) {
    displayRotationHelper.onPause()
  }

  fun startRendering() {
    activity.lifecycleScope.launch {
      loadResources()
      Log.d(TAG, "Resources loaded")
    }
    arSceneView.apply {
      lifecycle = activity.lifecycle
      configureSession { session, config ->
        config.depthMode = Config.DepthMode.DISABLED
        config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
        config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        planeRenderer.isEnabled = true
      }
      onTrackingFailureChanged = { reason ->
        Log.d(TAG, "Tracking failure reason: $reason")
      }
      onSessionUpdated = { _, frame ->
        updateFrame(frame)
      }
    }
  }

  private fun updateFrame(frame: Frame) {
    val session = arSceneView.session ?: return
    displayRotationHelper.updateSessionIfNeeded(session)

    if (frame.camera.trackingState != TrackingState.TRACKING) return
    if (isDetectionInProcess) return

    val cameraImage = frame.tryAcquireCameraImage()

    if (cameraImage == null) {
      Log.d(TAG, "Camera image is null")
      return
    }

    Log.d(TAG, "Camera image acquired, launching pose detection")

    isDetectionInProcess = true


//    val cameraId = session.cameraConfig.cameraId ?: return
    val imageRotation = 90 // displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
    Log.d(TAG, "Image rotation: $imageRotation")
    val inputImage = InputImage.fromMediaImage(cameraImage, imageRotation)

    poseDetector.process(inputImage)
      .addOnSuccessListener { detectedPose ->
        Log.d(TAG, "Pose detected successfully")
        onDetectedPose(detectedPose, frame, session)
      }
      .addOnFailureListener { e ->
        Log.e(TAG, "Pose detection failed", e)
      }
      .addOnCompleteListener {
        isDetectionInProcess = false
        cameraImage.close()
      }
  }

  private fun onDetectedPose(
    pose: com.google.mlkit.vision.pose.Pose,
    frame: Frame,
    session: Session
  ) {
    val leftShoulder =
      pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER)
    val rightShoulder =
      pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER)
    val leftHip = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP)
    val rightHip = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP)

    Log.v(TAG, "Left shoulder: $leftShoulder, right shoulder: $rightShoulder, left hip: $leftHip, right hip: $rightHip")
    var anchorPose = tryGetAnchorPose(frame, leftShoulder, rightHip)
    if (anchorPose == null) {
      anchorPose = tryGetAnchorPose(frame, rightShoulder, leftHip)
    }

    if (anchorPose != null && anchorNode == null){
      addAnchorNode(session.createAnchor(anchorPose))
    }
//    session.allAnchors.forEach { it.detach() }
  }

  private fun addAnchorNode(anchor: Anchor) {
    arSceneView.addChildNode(
      AnchorNode(arSceneView.engine, anchor)
        .apply {
          isEditable = true
          activity.lifecycleScope.launch {
            embroideryModel?.let { model ->
              val modelNode = ModelNode(
                modelInstance = model,
                scaleToUnits = 0.5f,
                centerOrigin = Position(y = -0.5f)
              ).apply {
                isEditable = true
              }
              addChildNode(modelNode)
            }
          }
          anchorNode = this
        }
    )
  }

  private fun tryGetAnchorPose(
    frame: Frame,
    shoulder: com.google.mlkit.vision.pose.PoseLandmark?,
    hip: com.google.mlkit.vision.pose.PoseLandmark?
  ): Pose? {
    if (shoulder == null || hip == null) return null
    val shoulderHit = getHitResult(frame, PointF(shoulder.position.x, shoulder.position.y))
    val hipHit = getHitResult(frame, PointF(hip.position.x, hip.position.y))
    if (shoulderHit == null || hipHit == null) return null
    return Pose.makeInterpolated(shoulderHit.hitPose, hipHit.hitPose, 0.5f)
  }

  private val convertFloats = FloatArray(4)
  private val convertFloatsOut = FloatArray(4)

  private fun getHitResult(frame: Frame, point: PointF): HitResult? {
    convertFloats[0] = point.x
    convertFloats[1] = point.y
    frame.transformCoordinates2d(
      Coordinates2d.IMAGE_PIXELS,
      convertFloats,
      Coordinates2d.VIEW,
      convertFloatsOut
    )
    return frame.hitTest(convertFloatsOut[0], convertFloatsOut[1]).firstOrNull()
  }

  private fun Frame.tryAcquireCameraImage(): Image? = try {
    acquireCameraImage()
  } catch (e: NotYetAvailableException) {
    null
  } catch (e: Throwable) {
    throw e
  }
}
