/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.vsu.arembroidery

import android.graphics.PointF
import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.examples.java.ml.render.LabelRender
import com.google.ar.core.examples.java.ml.render.PointCloudRender
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import ru.vsu.arembroidery.common.helpers.DisplayRotationHelper
import ru.vsu.arembroidery.common.samplerender.Mesh
import ru.vsu.arembroidery.common.samplerender.SampleRender
import ru.vsu.arembroidery.common.samplerender.Shader
import ru.vsu.arembroidery.common.samplerender.Texture
import ru.vsu.arembroidery.common.samplerender.arcore.BackgroundRenderer

/**
 * Renders the HelloAR application into using our example Renderer.
 */
class AppRenderer(val activity: MainActivity) : DefaultLifecycleObserver, SampleRender.Renderer, CoroutineScope by MainScope() {
  companion object {
    val TAG = "AppRenderer"
  }

  lateinit var view: MainActivityView

  val displayRotationHelper = DisplayRotationHelper(activity)
  lateinit var backgroundRenderer: BackgroundRenderer
  val pointCloudRender = PointCloudRender()
  val labelRenderer = LabelRender()

  val modelMatrix = FloatArray(16)
  val viewMatrix = FloatArray(16)
  val projectionMatrix = FloatArray(16)
  val modelViewMatrix = FloatArray(16)
  val viewProjectionMatrix = FloatArray(16)

  val poseDetector = PoseDetection.getClient(PoseDetectorOptions.Builder()
    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
    .build())

  private lateinit var planeTexture: Texture
  private lateinit var planeMesh: Mesh
  private lateinit var planeShader: Shader

  private var isDetectionInProcess = false

  override fun onResume(owner: LifecycleOwner) {
    displayRotationHelper.onResume()
  }

  override fun onPause(owner: LifecycleOwner) {
    displayRotationHelper.onPause()
  }

  fun bindView(view: MainActivityView) {
    this.view = view


  }

  override fun onSurfaceCreated(render: SampleRender) {
    backgroundRenderer = BackgroundRenderer(render).apply {
      setUseDepthVisualization(render, false)
    }
    pointCloudRender.onSurfaceCreated(render)
    labelRenderer.onSurfaceCreated(render)

    planeTexture =
      Texture.createFromAsset(
        render,
        "models/pawn_albedo.png",
        Texture.WrapMode.CLAMP_TO_EDGE,
        Texture.ColorFormat.SRGB
      )

    planeMesh = Mesh.createFromAsset(render, "models/pawn.obj")

    planeShader =
      Shader.createFromAssets(
        render,
        "shaders/plane_s.vert",
        "shaders/plane_s.frag",
        null
      ).setTexture("u_Texture", planeTexture)
  }

  override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
    displayRotationHelper.onSurfaceChanged(width, height)
  }

  private val anchorPosition = FloatArray(3)

  override fun onDrawFrame(render: SampleRender) {
    val session = activity.arCoreSessionHelper.sessionCache ?: return
    session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))

    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(session)

    val frame = try {
      session.update()
    } catch (e: CameraNotAvailableException) {
      Log.e(TAG, "Camera not available during onDrawFrame", e)
      showSnackbar("Camera not available. Try restarting the app.")
      return
    }

    backgroundRenderer.updateDisplayGeometry(frame)
    backgroundRenderer.drawBackground(render)

    // Get camera and projection matrices.
    val camera = frame.camera
    camera.getViewMatrix(viewMatrix, 0)
    camera.getProjectionMatrix(projectionMatrix, 0, 0.01f, 100.0f)
    Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

    // Handle tracking failures.
    if (camera.trackingState != TrackingState.TRACKING) {
      return
    }

    // Draw point cloud.
    frame.acquirePointCloud().use { pointCloud ->
      pointCloudRender.drawPointCloud(render, pointCloud, viewProjectionMatrix)
    }

    session.allAnchors.firstOrNull()?.let {
      Log.d(TAG, "Rendering model for anchor")

      val anchorNode = AnchorNode(it)
    }

    // Frame.acquireCameraImage must be used on the GL thread.
    // Check if the button was pressed last frame to start processing the camera image.
    if (isDetectionInProcess) {
      return
    }
    Log.d(TAG, "Initiating pose detection")

    val cameraImage = frame.tryAcquireCameraImage()

    if (cameraImage == null) {
      return
    }

    isDetectionInProcess = true

    val cameraId = session.cameraConfig.cameraId
    val imageRotation = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
    val inputImage = InputImage.fromMediaImage(cameraImage, imageRotation)

    poseDetector.process(inputImage)
      .addOnSuccessListener {
        Log.d(TAG, "Pose detected successfully")
        onDetectedPose(it, frame, session)
      }
      .addOnFailureListener {
        Log.e(TAG, "Pose detection failed", it)
      }
      .addOnCompleteListener {
        Log.d(TAG, "Pose detection finished")
        cameraImage.close()
        isDetectionInProcess = false
      }
  }

  /**
   * Utility method for [Frame.acquireCameraImage] that maps [NotYetAvailableException] to `null`.
   */
  fun Frame.tryAcquireCameraImage() = try {
    acquireCameraImage()
  } catch (e: NotYetAvailableException) {
    null
  } catch (e: Throwable) {
    throw e
  }

  private fun showSnackbar(message: String): Unit =
    activity.view.snackbarHelper.showError(activity, message)

  private fun hideSnackbar() = activity.view.snackbarHelper.hide(activity)

  private fun onDetectedPose(pose: Pose, frame: Frame, session: Session) {
    val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
    val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
    val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
    val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

    var pose =  tryGetAnchorPose(frame, leftShoulder, rightHip)

    if (pose == null) {
      pose = tryGetAnchorPose(frame, rightShoulder, leftHip)
    }

    session.allAnchors.forEach { it.detach() }

    pose?.let {
      session.createAnchor(it)
      
    }
  }

  private fun tryGetAnchorPose(frame: Frame, shoulder: PoseLandmark?, hip: PoseLandmark?) : com.google.ar.core.Pose? {
    if (shoulder == null || hip == null) {
      return null
    }
    shoulder.position
    val shoulderHit = getHitResult(frame, shoulder.position)
    val hipHit = getHitResult(frame, hip.position)

    if (shoulderHit == null || hipHit == null) {
      return null
    }

    return com.google.ar.core.Pose.makeInterpolated(shoulderHit.hitPose, hipHit.hitPose, 0.5f)
  }

  /**
   * Temporary arrays to prevent allocations in [getHitResult].
   */
  private val convertFloats = FloatArray(4)
  private val convertFloatsOut = FloatArray(4)

  private fun getHitResult(frame: Frame, point: PointF) : HitResult? {
    convertFloats[0] = point.x
    convertFloats[1] = point.y

    frame.transformCoordinates2d(
      Coordinates2d.IMAGE_PIXELS,
      convertFloats,
      Coordinates2d.VIEW,
      convertFloatsOut
    )

    val hits = frame.hitTest(convertFloatsOut[0], convertFloatsOut[1])

    return hits.getOrNull(0)
  }
}