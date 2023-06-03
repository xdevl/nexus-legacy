// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.agp)
        classpath(libs.kgp)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}