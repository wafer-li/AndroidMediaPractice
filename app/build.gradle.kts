import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.*

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

        externalNativeBuild {
            cmake {
                cppFlags.apply { add("-std=c++14") }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        this["main"].jni.srcDirs("src/main/cpp", "src/main/jni")
    }

    externalNativeBuild {
        cmake {
            setPath("CMakeLists.txt")
            setVersion("3.10.2")
        }
    }


    lintOptions {
        disable("GoogleAppIndexingWarning")
        disable("AllowBackup")
    }

    signingConfigs {
        create("default") {
            val properties = Properties().apply {
                val f = project.rootProject.file("local.properties")
                if (f.exists()) {
                    load(f.inputStream())
                }
            }

            storeFile = file("wafer-keystore.keystore")
            storePassword =
                if (properties.hasProperty("KEY_STORE_PASS")) properties.getProperty("KEY_STORE_PASS") else System.getenv(
                    "KEY_STORE_PASS"
                )
            keyAlias =
                if (properties.hasProperty("KEY_ALIAS")) properties.getProperty("KEY_ALIAS") else System.getenv(
                    "KEY_ALIAS"
                )
            keyPassword =
                if (properties.hasProperty("KEY_PASS")) properties.getProperty("KEY_PASS") else System.getenv(
                    "KEY_PASS"
                )
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("default")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("default")
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

    /**
     * Permission
     */
    implementation(Libs.permissionsdispatcher)
    kapt(Libs.permissionsdispatcher_processor)
}
