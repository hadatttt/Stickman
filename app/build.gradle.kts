plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("kotlin-kapt")  // cần có để kapt hoạt động với Room
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.hadat.stickman"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hadat.stickman"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
        viewBinding = true
    }
}

dependencies {
    // AndroidX core libs
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Unit tests
    testImplementation(libs.junit)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Glide (image loading)
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // Color Picker View
    implementation("com.github.skydoves:colorpickerview:2.2.4")

    // Room
    val room_version = "2.7.2"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")          // <-- sửa thành kapt
    implementation("androidx.room:room-ktx:$room_version")

    // Nếu bạn dùng RxJava (nếu không dùng thì bỏ)
    implementation("androidx.room:room-rxjava2:$room_version")
    implementation("androidx.room:room-rxjava3:$room_version")

    // Guava support (tuỳ chọn)
    implementation("androidx.room:room-guava:$room_version")

    // Test helpers (tuỳ chọn)
    testImplementation("androidx.room:room-testing:$room_version")

    // Paging 3 integration (tuỳ chọn)
    implementation("androidx.room:room-paging:$room_version")

    implementation("com.google.code.gson:gson:2.11.0")
}
