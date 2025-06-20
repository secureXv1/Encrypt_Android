pluginManagement {
    plugins {
        id("androidx.compose.compiler") version("1.5.13") // ✅ Obligatorio para Kotlin 2.0+
    }

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Encrypt_Android"
include(":app")
