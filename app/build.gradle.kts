apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'

android {
    compileSdkVersion 32
    defaultConfig {
        applicationId "com.xdevl.wallpaper.nexus"
        minSdkVersion 23
        targetSdkVersion 32
        versionCode 2
        versionName "2.0"
        setProperty("archivesBaseName", "NexusLegacy-$versionName")
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    testOptions.unitTests.includeAndroidResources true
}

/** project.androidComponents {
    beforeVariants({ variant ->
        println("Called with variant : ${variant}name")
    })
} **/

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4"
    implementation "androidx.preference:preference-ktx:1.2.0"
    implementation "com.jakewharton.timber:timber:5.0.1"

    testImplementation "junit:junit:4.13.2"
    testImplementation "androidx.test.ext:junit:1.1.2"
    testImplementation "com.google.truth:truth:1.1.3"
    testImplementation "org.robolectric:robolectric:4.10.2"
}
