include("app")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("agp", "com.android.tools.build:gradle:8.0.2")
            library("kgp", "org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
        }
    }
}