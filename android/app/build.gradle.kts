plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Compose Core UI
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Compose Material (Material 3 + Material 보완)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Compose Foundation (Modifier, Column, Row, Spacer 등)
    implementation("androidx.compose.foundation:foundation:1.5.0")

    // Compose 단위 관련 (dp, sp 등)
    implementation("androidx.compose.ui:ui-unit:1.5.0") // 일부 버전에서는 생략 가능

    // 이미지 로딩 및 업로드 관련
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(libs.androidx.navigation.common.android)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.monitor)
    implementation(libs.core.ktx)
    //implementation(libs.litert.support.api)

    // 이미지 전처리
    implementation(libs.play.services.mlkit.face.detection)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    //implementation(libs.litert.support.api)


    // 테스트
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // 디버그 전용
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 모델 추론을 위해 TFLite 라이브러리 추가
    implementation("org.tensorflow:tensorflow-lite:2.13.0")

    // TensorBuffer 사용을 위한 support 라이브러리 추가
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // mediapipe 사용을 위한 라이브러리 추가
    // 반드시 명시적으로 선언
    implementation("com.google.mediapipe:tasks-vision:0.20230731")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.3")



}
