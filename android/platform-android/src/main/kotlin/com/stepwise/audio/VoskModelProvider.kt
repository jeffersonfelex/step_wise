package com.stepwise.audio

import android.content.Context
import android.util.Log
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vosk.Model
import org.vosk.android.StorageService

/**
 * Carrega (uma única vez por processo) o modelo Vosk embutido em
 * assets/model. Não é baixado em tempo de execução: o download acontece
 * antes do build (ver scripts/download_vosk_model.sh ou a tarefa Gradle
 * downloadVoskModel em :platform-android).
 *
 * `StorageService.unpack` descompacta assets/model para o armazenamento
 * interno do app (uma vez) e devolve o [Model] já carregado.
 */
object VoskModelProvider {

    private const val TAG = "VoskModelProvider"
    private const val ASSET_MODEL_DIR = "model"
    private const val UNPACKED_MODEL_DIR = "model"

    @Volatile
    private var cachedModel: Model? = null
    private val mutex = Mutex()

    val isReady: Boolean
        get() = cachedModel != null

    /**
     * Retorna o modelo já carregado (rápido, após a primeira chamada) ou
     * suspende até terminar de descompactar/carregar. Retorna null se
     * assets/model não existir (ex.: download não foi rodado antes do
     * build) — o chamador deve tratar isso como KWS indisponível, nunca
     * cair para o SpeechRecognizer na nuvem.
     */
    suspend fun awaitModel(context: Context): Model? {
        cachedModel?.let { return it }
        return mutex.withLock {
            cachedModel?.let { return@withLock it }
            val loaded = suspendCancellableCoroutine<Model?> { cont ->
                StorageService.unpack(
                    context.applicationContext,
                    ASSET_MODEL_DIR,
                    UNPACKED_MODEL_DIR,
                    { model: Model ->
                        if (cont.isActive) cont.resume(model)
                    },
                    { exception: java.io.IOException ->
                        Log.e(
                            TAG,
                            "Falha ao carregar modelo Vosk. Confirme que assets/$ASSET_MODEL_DIR " +
                                "existe (rode scripts/download_vosk_model.sh ou " +
                                "./gradlew :platform-android:downloadVoskModel antes do build).",
                            exception
                        )
                        if (cont.isActive) cont.resume(null)
                    }
                )
            }
            cachedModel = loaded
            loaded
        }
    }

    /** Pré-carrega o modelo em background, sem bloquear quem chama (ex.: Application.onCreate). */
    fun preload(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            awaitModel(context)
        }
    }
}
