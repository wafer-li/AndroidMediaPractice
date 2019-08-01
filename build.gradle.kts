// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.4.2")
        classpath(kotlin("gradle-plugin", Versions.org_jetbrains_kotlin))
        classpath ("androidx.navigation:navigation-safe-args-gradle-plugin:2.1.0-beta02")
        classpath ("org.jetbrains.kotlin:kotlin-serialization:${Versions.org_jetbrains_kotlin}")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    id("de.fayard.buildSrcVersions") version "0.3.2"
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks {
    val clean by registering(Delete::class) {
        delete(buildDir)
    }
}
