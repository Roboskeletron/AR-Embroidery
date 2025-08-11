package ru.vsu.arembroidery.di

import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.passay.CharacterRule
import org.passay.EnglishCharacterData
import org.passay.LengthRule
import org.passay.PasswordValidator
import org.passay.WhitespaceRule
import ru.vsu.arembroidery.usecases.CreateWarpedBitmapUseCase
import ru.vsu.arembroidery.usecases.TransformLandMarkUseCase
import ru.vsu.arembroidery.viewmodels.TryOnFragmentVM

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
    single {
        PasswordValidator(
            listOf(
                LengthRule(8, 30),
                CharacterRule(EnglishCharacterData.UpperCase, 1),
                CharacterRule(EnglishCharacterData.LowerCase, 1),
                CharacterRule(EnglishCharacterData.Special, 1),
                CharacterRule(EnglishCharacterData.Digit, 1),
                WhitespaceRule()
            )
        )
    }
    viewModelOf(::TryOnFragmentVM)
}
