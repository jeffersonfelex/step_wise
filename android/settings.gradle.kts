import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = Properties().apply {
    val file = File(rootDir, "local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Token classico do GitHub (escopo read:packages) para baixar o Meta
// Wearables Device Access Toolkit via GitHub Packages - nunca commitado,
// lido de local.properties (github_token=...) ou da variavel de ambiente
// GITHUB_TOKEN. Sem ele, com.meta.wearable:mwdat-* (usado por
// MetaDatSessionManager em :platform-android) nao resolve.
val githubToken: String = (localProperties.getProperty("github_token")
    ?: System.getenv("GITHUB_TOKEN")
    ?: "").trim()
val githubUsername: String = (localProperties.getProperty("github_username")
    ?: System.getenv("GITHUB_ACTOR")
    ?: "stepwise").trim()

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Repositorio de distribuicao do Meta Wearables Device Access
        // Toolkit (com.meta.wearable:mwdat-core / mwdat-camera), via GitHub
        // Packages - exige autenticacao mesmo para o repositorio publico.
        // Configure github_token (PAT classico, escopo read:packages) em
        // local.properties (ver local.properties.example).
        maven {
            url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
            credentials {
                username = githubUsername
                password = githubToken
            }
        }

        // Repositório oficial do Vosk (ASR offline) - com.alphacephei:vosk-android.
        // Usado por :platform-android (ver KeywordSpotter/OfflineKeywordSpotter).
        maven { url = uri("https://alphacephei.com/maven/") }
    }
}

rootProject.name = "stepwise"
include(":app", ":core", ":platform-android", ":mode-hardware", ":mode-software")
include(":software-android")
