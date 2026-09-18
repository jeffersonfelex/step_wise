// The software team needs only JDK 17, not Android SDK or Meta DAT.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins { id("org.jetbrains.kotlin.jvm") version "1.9.24" }
}

dependencyResolutionManagement { repositories { mavenCentral() } }

rootProject.name = "stepwise-jvm-tests"
include(":core", ":mode-software")
project(":core").projectDir = file("../core")
project(":mode-software").projectDir = file("../mode-software")
