plugins {
    id("com.android.application")
    id("maven-publish")
    kotlin("android")
}

android {
    namespace = "com.xdevl.wallpaper.nexus"
    compileSdk = 32
    defaultConfig {
        minSdk = 23
        targetSdk = 32
        versionCode = 2
        versionName = "2.0"
        setProperty("archivesBaseName", "NexusLegacy-$versionName")
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("com.jakewharton.timber:timber:5.0.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.1.2")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("org.robolectric:robolectric:4.10.2")
}
