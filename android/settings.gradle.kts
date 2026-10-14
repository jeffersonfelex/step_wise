pluginManagement {
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
        // Maven Central também distribui o Meta Wearables DAT (com.meta.wearable:mwdat-*)
        // desde 14/09/2026 e o Vosk (com.alphacephei:vosk-android): nenhum token
        // do GitHub Packages ou repositório extra é necessário.
        mavenCentral()
    }
}

rootProject.name = "stepwise"
include(":app", ":core", ":platform-android", ":mode-hardware", ":mode-software")
include(":software-android")
