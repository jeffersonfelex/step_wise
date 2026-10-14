// Loads the Kotlin plugin once for both JVM modules (avoids duplicate classloaders).
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}
