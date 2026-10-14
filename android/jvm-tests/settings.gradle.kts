// The software team needs only JDK 17, not Android SDK or Meta DAT.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories { mavenCentral() }
    // Same versions as the Android build; no duplicated version numbers.
    versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}

rootProject.name = "stepwise-jvm-tests"
include(":core", ":mode-software")
project(":core").projectDir = file("../core")
project(":mode-software").projectDir = file("../mode-software")
