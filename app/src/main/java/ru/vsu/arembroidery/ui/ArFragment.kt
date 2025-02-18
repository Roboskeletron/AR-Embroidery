package ru.vsu.arembroidery.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.ar.core.Config
import com.google.ar.core.Config.InstantPlacementMode
import com.google.ar.core.Session
import com.google.ar.core.examples.kotlin.common.helpers.ARCoreSessionLifecycleHelper
import com.google.ar.core.examples.kotlin.helloar.HelloArRenderer
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import ru.vsu.arembroidery.ar.helpers.DepthSettings
import ru.vsu.arembroidery.ar.helpers.FullScreenHelper
import ru.vsu.arembroidery.ar.helpers.InstantPlacementSettings
import ru.vsu.arembroidery.ar.helpers.SnackbarHelper
import ru.vsu.arembroidery.ar.helpers.TapHelper
import ru.vsu.arembroidery.ar.samplerender.SampleRender
import ru.vsu.arembroidery.databinding.FragmentArBinding

class ArFragment : Fragment() {

    lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper
    private lateinit var binding: FragmentArBinding
    lateinit var tapHelper: TapHelper
    lateinit var renderer: HelloArRenderer

    val snackbarHelper = SnackbarHelper()

    private val viewModel: ArViewModel by viewModels()

    val instantPlacementSettings = InstantPlacementSettings()
    val depthSettings = DepthSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentArBinding.inflate(inflater, container, false)

        arCoreSessionHelper = ARCoreSessionLifecycleHelper(activity)

        arCoreSessionHelper.exceptionCallback =
            { exception ->
                val message =
                    when (exception) {
                        is UnavailableUserDeclinedInstallationException ->
                            "Please install Google Play Services for AR"
                        is UnavailableApkTooOldException -> "Please update ARCore"
                        is UnavailableSdkTooOldException -> "Please update this app"
                        is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                        is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                        else -> "Failed to create AR session: $exception"
                    }
                Log.e(ArFragment::class.simpleName, "ARCore threw an exception", exception)
                snackbarHelper.showError(requireActivity(), message)
            }

        arCoreSessionHelper.beforeSessionResume = ::configureSession
        lifecycle.addObserver(arCoreSessionHelper)

        // Set up the Hello AR renderer.
        renderer = HelloArRenderer(this)
        lifecycle.addObserver(renderer)

        tapHelper = TapHelper(requireActivity()).also { binding.surfaceview.setOnTouchListener(it) }

        lifecycle.addObserver(arCoreSessionHelper)

        // Set up the Hello AR renderer.
        renderer = HelloArRenderer(this)
        lifecycle.addObserver(renderer)

        SampleRender(binding.surfaceview, renderer, requireActivity().assets)
        depthSettings.onCreate(context)
        instantPlacementSettings.onCreate(context)

        return binding.root
    }

    private fun configureSession(session: Session) {
        session.configure(
            session.config.apply {
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

                depthMode = Config.DepthMode.DISABLED
//                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
//                        Config.DepthMode.AUTOMATIC
//                    } else {
//                        Config.DepthMode.DISABLED
//                    }

                instantPlacementMode =
                    if (instantPlacementSettings.isInstantPlacementEnabled) {
                        InstantPlacementMode.LOCAL_Y_UP
                    } else {
                        InstantPlacementMode.DISABLED
                    }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        binding.surfaceview.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.surfaceview.onPause()
    }
}