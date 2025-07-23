package ru.vsu.arembroidery.di

import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.vsu.arembroidery.usecases.CreateWarpedBitmapUseCase
import ru.vsu.arembroidery.usecases.TransformLandMarkUseCase
import ru.vsu.arembroidery.views.TryOnFragmentVM

val appModule = module {
    single {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .setPreferredHardwareConfigs(PoseDetectorOptions.CPU_GPU)
                .build()
        )
    }
    single { TransformLandMarkUseCase() }
    single { CreateWarpedBitmapUseCase(get(), get()) }
    viewModelOf(::TryOnFragmentVM)
}