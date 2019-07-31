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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-RC")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.0-RC")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.11.1")



    /**
     * Architecture Component
     */
    // Concurrent
    implementation("androidx.concurrent:concurrent-futures:1.0.0-beta01")
    implementation("androidx.concurrent:concurrent-listenablefuture:1.0.0-beta01")
    implementation("androidx.concurrent:concurrent-listenablefuture-callback:1.0.0-beta01")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-extensions:2.0.0")
    annotationProcessor("androidx.lifecycle:lifecycle-compiler:2.0.0")
    testImplementation("androidx.arch.core:core-testing:2.0.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.1.0-beta02")
    implementation("androidx.navigation:navigation-ui-ktx:2.1.0-beta02")

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:2.1.0")
    testImplementation("androidx.paging:paging-common-ktx:2.1.0")

    // Room
    implementation("androidx.room:room-runtime:2.2.0-alpha01")
    kapt("androidx.room:room-compiler:2.2.0-alpha01")
    implementation("androidx.room:room-ktx:2.2.0-alpha01")
    testImplementation("androidx.room:room-testing:2.2.0-alpha01")

    // Work manager
    implementation("androidx.work:work-runtime-ktx:2.1.0")
    androidTestImplementation("androidx.work:work-testing:2.1.0")
}
