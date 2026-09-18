pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Repositório de distribuição do Meta DAT (Device Access Toolkit) -
        // ajustar conforme instruções oficiais do SDK Ray-Ban Meta.
        // maven { url = uri("https://<meta-dat-maven-repo>") }
    }
}

rootProject.name = "stepwise"
include(":app", ":core", ":platform-android", ":mode-hardware", ":mode-software")
