plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.stepwise"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stepwise"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-mvp"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    // Persistência local (Room) - estado da sessão, eventos mínimos.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Detector visual local - PLANO A (placeholder - confirmar artefato oficial).
    // implementation("com.google.mediapipe:tasks-vision:LATEST")
    // implementation("com.google.ai.edge.litert:litert:LATEST")

    // Detector visual local - PLANO B (deterministico, ver com.stepwise.vision):
    //  - ColorSegmentationDetector usa apenas android.graphics.Color/Bitmap,
    //    sem dependência extra.
    //  - ArucoMarkerDetector: BoofCV é pure-Kotlin/Java (sem binários nativos),
    //    mais simples de integrar sob pressão de tempo do que OpenCV+aruco.
    // implementation("org.boofcv:boofcv-core:1.1.4")
    // implementation("org.boofcv:boofcv-android:1.1.4")

    // Meta DAT (Device Access Toolkit) - SDK do Ray-Ban Meta Gen 2.
    // implementation("com.meta.dat:device-access-toolkit:0.9.0")

    // ASR offline (privacidade - ver com.stepwise.audio.KeywordSpotter).
    // Escolher UM: sherpa-onnx (KWS), Vosk (gramática restrita) ou Porcupine.
    // implementation("com.k2fsa.sherpa.onnx:sherpa-onnx-android:LATEST")
    // implementation("net.java.dev.jna:jna:5.13.0@aar") // dependência comum do Vosk Android
    // implementation("ai.picovoice:porcupine-android:LATEST")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
