import java.security.MessageDigest
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Chave da API Groq usada por com.stepwise.ai.LlamaVisionClient. Nunca
// commitada: lida de local.properties (groq.api.key=...) ou da variável de
// ambiente GROQ_API_KEY. Entra SOMENTE no build debug: qualquer chave dentro
// de um APK pode ser extraída, então o release nunca a embute (o cliente trata
// a ausência como "IA indisponível", sem quebrar a sessão).
val groqApiKey: String = (localProperties.getProperty("groq.api.key")
    ?: System.getenv("GROQ_API_KEY")
    ?: "").trim()

android {
    namespace = "com.stepwise.platform"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt() // exigido pelo Meta Wearables DAT
    }
    buildTypes {
        debug { buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"") }
        release { buildConfigField("String", "GROQ_API_KEY", "\"\"") }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(project(":core"))

    // ASR offline com gramática restrita (OfflineKeywordSpotter) - substitui o
    // SpeechRecognizer padrão do Android, que pode enviar áudio a servidores do
    // Google (decisão registrada em docs/lgpd-privacidade.md). O modelo é
    // baixado por scripts/download_vosk_model.sh ou pela tarefa Gradle
    // downloadVoskModel (definida abaixo) - nunca em tempo de execução.
    implementation(libs.vosk.android)
    implementation(libs.jna) { artifact { type = "aar" } }

    // com.stepwise.ai.LlamaVisionClient (Groq, modelos com visão).
    implementation(libs.okhttp)

    // Meta Wearables Device Access Toolkit (Ray-Ban Meta Gen 2) - sessão real
    // com os óculos e captura de foto (ver com.stepwise.glasses.MetaDatSessionManager).
    implementation(libs.mwdat.core)
    implementation(libs.mwdat.camera)

    // Orientação EXIF da foto HEIC entregue pelo capturePhoto() dos óculos.
    implementation(libs.androidx.exifinterface)

    testImplementation(libs.junit)
}

// ---------------------------------------------------------------------------
// Modelo Vosk (ASR offline) - download automático para src/main/assets/model
// ---------------------------------------------------------------------------
//
// Baixa e descompacta o modelo antes do build, para que fique embutido no APK
// e o app não dependa de download em tempo de execução. Equivalente a
// scripts/download_vosk_model.sh (raiz do repositório). Idempotente, e o zip
// só é aceito se o SHA-256 bater com o fixado abaixo.
val voskModelName = "vosk-model-small-pt-0.3"
val voskModelUrl = "https://alphacephei.com/vosk/models/$voskModelName.zip"
val voskModelSha256 = "6e1ce909032e1afa7a88e68a3d628ecafff302bdf195befab308826c395e93b7"
val voskModelAssetsDir = file("src/main/assets/model")

tasks.register("downloadVoskModel") {
    group = "stepwise"
    description = "Baixa o modelo Vosk ($voskModelName) para src/main/assets/model."

    val versionMarker = File(voskModelAssetsDir, ".vosk-model-version")
    val zipFileProvider = layout.buildDirectory.file("tmp/vosk/$voskModelName.zip")
    val extractDirProvider = layout.buildDirectory.dir("tmp/vosk/extracted")

    doLast {
        if (versionMarker.exists() && versionMarker.readText().trim() == voskModelName) {
            logger.lifecycle("StepWise: modelo Vosk já presente em ${voskModelAssetsDir.path}, pulando download.")
            return@doLast
        }

        val zip = zipFileProvider.get().asFile
        zip.parentFile.mkdirs()
        logger.lifecycle("StepWise: baixando modelo Vosk de $voskModelUrl ...")
        ant.withGroovyBuilder {
            "get"("src" to voskModelUrl, "dest" to zip, "verbose" to false, "usetimestamp" to true)
        }

        val digest = MessageDigest.getInstance("SHA-256")
        zip.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != voskModelSha256) {
            zip.delete()
            throw GradleException("SHA-256 do modelo Vosk não confere (esperado $voskModelSha256, obtido $actual).")
        }

        val extractRoot = extractDirProvider.get().asFile
        if (extractRoot.exists()) extractRoot.deleteRecursively()
        extractRoot.mkdirs()
        logger.lifecycle("StepWise: descompactando modelo...")
        ant.withGroovyBuilder {
            "unzip"("src" to zip, "dest" to extractRoot)
        }

        if (voskModelAssetsDir.exists()) voskModelAssetsDir.deleteRecursively()
        voskModelAssetsDir.mkdirs()
        val extractedModelDir = File(extractRoot, voskModelName)
        extractedModelDir.copyRecursively(voskModelAssetsDir, overwrite = true)

        versionMarker.writeText(voskModelName)
        logger.lifecycle("StepWise: modelo Vosk pronto em ${voskModelAssetsDir.path}.")
    }
}

tasks.named("preBuild") {
    dependsOn("downloadVoskModel")
}
