@file:Suppress("UnresolvedReference")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.dagger.hilt)
}

android {
    namespace = "com.otimiza.delivery"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.otimiza.delivery"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.otimiza.delivery.DeliveryTestRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas".toString())
            }
        }
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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtension = libs.versions.compose.get()
    }

    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/{AL2.0,LGPL2.1,LGPL2.0,LGPL2.1,kotlinx_coroutines_core.version}",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

hilt {
    enableAggregatingTask = true
}

dependencies {
    // Core
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.bom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Lifecycle / coroutines
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.hilt.navigation.compose)

    // DI
    implementation(libs.dagger.hilt)
    kapt(libs.dagger.hilt.kapt)

    // Persistence (Room) — composite PK entity layer
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.kapt)

    // CameraX + ML Kit (edge OCR for physical labels)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text)

    // Networking (Retrofit/OkHttp -> VRP engine)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Mapa unificado (MapLibre)
    implementation(libs.maplibre.maps)

    // ---- Testes (unitários) — JUnit 5 + MockK 1.14.11 ----
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.bdd)
}

tasks.test {
    useJUnitPlatform()
    // Otimiza concorrência de testes — align com qa-tester (parallel-safe mocks)
    maxParallelForks = Runtime.getRuntime().availableProcessors().coerceAtMost(4)
}
