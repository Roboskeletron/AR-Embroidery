package ru.vsu.arembroidery.views

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.pose.PoseDetector
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import ru.vsu.arembroidery.R
import ru.vsu.arembroidery.analyzers.PoseDetectionAnalyzer
import ru.vsu.arembroidery.data.MatrixRepository
import ru.vsu.arembroidery.databinding.FragmentTryOnBinding

class TryOnFragment : Fragment() {

    companion object{
        private const val TAG = "TryOnFragment"
    }

    private lateinit var binding: FragmentTryOnBinding

    private val poseDetector by inject<PoseDetector>()
    private val matrixRepository by inject<MatrixRepository>()
    private val viewModel by viewModel<TryOnFragmentVM>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTryOnBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.scaleSlider.addOnChangeListener { _, value, _ ->
            viewModel.scale = value / 100.0
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadEmbroidery()
        startCamera()
    }

    private fun loadEmbroidery() {
        val embroideryBitmap = BitmapFactory.decodeResource(resources, R.drawable.example_texture)

        val embroideryMat = Mat(embroideryBitmap.height, embroideryBitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(embroideryBitmap, embroideryMat)
        matrixRepository.updateEmbroideryMat(embroideryMat)
    }

    private fun startCamera() {
        binding.cameraPreview.controller = LifecycleCameraController(requireContext()).apply {
            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(requireContext()),
                PoseDetectionAnalyzer(
                    poseDetector,
                    ContextCompat.getMainExecutor(requireContext())
                ) { result ->
                    binding.cameraPreview.apply {
                        overlay.clear()
                        viewModel.processPoseAnalysisResult(result).forEach { overlay.add(it) }
                    }
                }
            )

            bindToLifecycle(viewLifecycleOwner)
        }

        binding.cameraPreview.addOnLayoutChangeListener{ layout, _, _, _, _, _, _, _, _ ->
            layout.apply {
                matrixRepository.updateWarpedEmbroideryMat(width, height)
                viewModel.apply {
                    previewWidth.value = width
                    previewHeight.value = height
                }
                if (width > 0) {
                    binding.offsetXSlider.apply {
                        valueFrom = -binding.cameraPreview.width / 2f
                        valueTo = -valueFrom
                    }

                    binding.offsetYSlider.apply {
                        valueFrom = -binding.cameraPreview.height / 2f
                        valueTo = -valueFrom
                    }
                }
            }
        }
    }

}