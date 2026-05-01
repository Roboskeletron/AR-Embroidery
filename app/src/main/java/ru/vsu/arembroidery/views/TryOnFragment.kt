package ru.vsu.arembroidery.views

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.pose.PoseDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import ru.vsu.arembroidery.R
import ru.vsu.arembroidery.analyzers.PoseDetectionAnalyzer
import ru.vsu.arembroidery.data.MatrixRepository
import ru.vsu.arembroidery.databinding.FragmentTryOnBinding
import ru.vsu.arembroidery.di.GlideApp
import ru.vsu.arembroidery.usecases.LoadEmbroideryUseCase
import ru.vsu.arembroidery.viewmodels.TryOnFragmentVM

class TryOnFragment : Fragment() {

    companion object{
        private const val TAG = "TryOnFragment"
    }

    private lateinit var binding: FragmentTryOnBinding

    private val poseDetector by inject<PoseDetector>()
    private val matrixRepository by inject<MatrixRepository>()
    private val viewModel by viewModel<TryOnFragmentVM>()
    private val loadEmbroideryUseCase by inject<LoadEmbroideryUseCase>()

    private var hideToolbarJob: Job? = null

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

        binding.embroideryImageView.setOnClickListener {
            findNavController().navigate(TryOnFragmentDirections.actionTryOnFragmentToDesignsFragment())
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadEmbroidery()
        startCamera()
        setupPhotoSavedObserver()
    }

    private fun loadEmbroidery() {
        val bitmap = loadEmbroideryUseCase.invoke(requireContext())
        binding.embroideryImageView.setImageBitmap(bitmap)
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

        binding.takePictureButton.setOnClickListener {
            takePicture()
        }
    }

    private fun takePicture() = binding.cameraPreview.bitmap?.let { bitmap ->
        viewModel.takePicture(requireContext().contentResolver, bitmap)
    }

    private fun setupPhotoSavedObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.photoCaptureEvent.collect { uri ->
                GlideApp.with(this@TryOnFragment)
                    .load(uri)
                    .into(binding.photoThumbnail)

                binding.photoSavedToolbar.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f).setDuration(300).start()
                }

                binding.viewPhotoButton.setOnClickListener {
                    findNavController().navigate(
                        TryOnFragmentDirections.actionTryOnFragmentToPhotoReviewFragment(uri.toString())
                    )
                }

                hideToolbarJob?.cancel()
                hideToolbarJob = launch {
                    delay(5000)
                    binding.photoSavedToolbar.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            binding.photoSavedToolbar.visibility = View.GONE
                        }
                        .start()
                }
            }
        }
    }
}
