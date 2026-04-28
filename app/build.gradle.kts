plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "ru.vsu.arembroidery"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.vsu.arembroidery"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://api.embroidery-portal.usr0.ru/\"")
        buildConfigField("boolean", "SHOW_POSE_DEBUG_OVERLAY", "false")
        buildConfigField("String", "KEYCLOAK_ISSUER_URI", "\"https://auth.embroidery-portal.usr0.ru/realms/portal-for-embroidery\"")
        buildConfigField("String", "CLIENT_ID", "\"portal-for-embroidery-android\"")

        manifestPlaceholders["appAuthRedirectScheme"] = "ru.vsu.arembroidery"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }
}

dependencies {
    implementation(libs.appauth)

    implementation(libs.insert.koin.koin.android)

    implementation(libs.androidx.datastore.preferences)

    // Network dependencies - Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    implementation(libs.androidx.paging.runtime)

    implementation(libs.glide)
    implementation(libs.okhttp3.integration)
    kapt(libs.compiler)

    // Coroutines for async operations
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.mlkit.pose.detection)
    implementation(libs.mlkit.pose.detection.accurate)
    implementation(libs.androidx.camera.mlkit.vision)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(project(":opencv"))
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.fragment.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Password validation
    implementation(libs.passay)
}