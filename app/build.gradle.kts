import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("kotlinx-serialization")

    id("androidx.navigation.safeargs.kotlin")
}

android {
    compileSdkVersion(BuildVersions.targetSdkVersion)
    buildToolsVersion(BuildVersions.buildToolsVersion)
    defaultConfig {
        applicationId = "com.example.androidmediapractice"
        minSdkVersion(BuildVersions.minSdkVersion)
        targetSdkVersion(BuildVersions.targetSdkVersion)
        versionCode = BuildVersions.versionCode
        versionName = BuildVersions.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation(Libs.appcompat)
    implementation(Libs.core_ktx)
    implementation(Libs.constraintlayout)
    testImplementation(Libs.junit_junit)
    androidTestImplementation(Libs.androidx_test_ext_junit)
    androidTestImplementation(Libs.espresso_core)

    /**
     * Kotlin And Kotlinx
     */
    implementation(kotlin("stdlib-jdk7", KotlinCompilerVersion.VERSION))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_android)
    implementation(Libs.kotlinx_serialization_runtime)



    /**
     * Architecture Component
     */
    // Concurrent
    implementation(Libs.concurrent_futures)
    implementation(Libs.concurrent_listenablefuture)
    implementation(Libs.concurrent_listenablefuture_callback)

    // ViewModel and LiveData
    implementation(Libs.lifecycle_extensions)
    kapt(Libs.lifecycle_compiler)
    testImplementation(Libs.core_testing)

    // Navigation
    implementation(Libs.navigation_fragment_ktx)
    implementation(Libs.navigation_ui_ktx)

    // Paging
    implementation(Libs.paging_runtime_ktx)
    testImplementation(Libs.paging_common_ktx)

    // Room
    implementation(Libs.room_runtime)
    kapt(Libs.room_compiler)
    implementation(Libs.room_ktx)
    testImplementation(Libs.room_testing)

    // Work manager
    implementation(Libs.work_runtime_ktx)
    androidTestImplementation(Libs.work_testing)
}
