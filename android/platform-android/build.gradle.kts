import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Chave da API Groq (Llama 4 Scout/Maverick com visao) usada por
// com.stepwise.ai.LlamaVisionClient. Nunca commitada: lida de
// local.properties (groq.api.key=...), com fallback para a variavel de
// ambiente GROQ_API_KEY (util em CI). Vazia se nenhuma das duas existir -
// o cliente trata isso como erro de configuracao, nao crasha o build.
val groqApiKey: String = (localProperties.getProperty("groq.api.key")
    ?: System.getenv("GROQ_API_KEY")
    ?: "").trim()

android {
    namespace = "com.stepwise.platform"
    compileSdk = 35
    defaultConfig {
        minSdk = 31 // exigido pelo Meta Wearables DAT (mwdat-core/mwdat-camera)
        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))
    // Meta DAT and audio runtimes belong here when integrated.

    // ASR offline com gramática restrita (OfflineKeywordSpotter) - substitui o
    // SpeechRecognizer padrão do Android, que pode enviar áudio a servidores do
    // Google (decisão registrada em docs/lgpd-privacidade.md). O modelo é
    // baixado por scripts/download_vosk_model.sh ou pela tarefa Gradle
    // downloadVoskModel (definida abaixo) - nunca em tempo de execução.
    implementation("com.alphacephei:vosk-android:0.3.47")
    implementation("net.java.dev.jna:jna:5.13.0@aar")

    // com.stepwise.ai.LlamaVisionClient (Groq, modelos Llama 4 com visao,
    // comparacao de cada frame ao vivo com o desenho tecnico de referencia).
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Meta Wearables Device Access Toolkit (Ray-Ban Meta Gen 2) - sessao real
    // com os oculos e captura de foto via DeviceSession/Camera/Stream (ver
    // com.stepwise.glasses.MetaDatSessionManager). Repositorio + credenciais
    // (github_token) configurados em settings.gradle.kts.
    implementation("com.meta.wearable:mwdat-core:0.9.0")
    implementation("com.meta.wearable:mwdat-camera:0.9.0")

    // Leitura do EXIF (orientacao) da foto HEIC entregue pelo capturePhoto()
    // dos oculos - sem isso a imagem enviada para a IA pode vir de lado.
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    testImplementation("junit:junit:4.13.2")
}

// ---------------------------------------------------------------------------
// Modelo Vosk (ASR offline) - download automático para src/main/assets/model
// ---------------------------------------------------------------------------
//
// Baixa e descompacta o modelo antes do build, para que fique embutido no APK
// e o app não dependa de download em tempo de execução. Equivalente a
// scripts/download_vosk_model.sh (raiz do repositório), para quem prefere
// rodar direto do Android Studio / `./gradlew`. Idempotente.
val voskModelName = "vosk-model-small-pt-0.3"
val voskModelUrl = "https://alphacephei.com/vosk/models/$voskModelName.zip"
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
            "get"("src" to voskModelUrl, "dest" to zip, "verbose" to true, "usetimestamp" to true)
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
