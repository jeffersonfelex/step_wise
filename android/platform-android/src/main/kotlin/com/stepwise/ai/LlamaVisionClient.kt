package com.stepwise.ai

import android.util.Base64
import android.util.Log
import com.stepwise.platform.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cliente HTTP para a API de chat da Groq (compatível com o formato
 * OpenAI) — a Meta Llama API oficial (llama.developer.meta.com) foi
 * descontinuada em 2026 (ver checklist-hackathon.md, critério "Uso de
 * IA": modelo nomeado, entrada externa muda a saída).
 *
 * HISTÓRICO/CORREÇÃO (18/09/2026, em produção durante o hackathon):
 * 1) A ideia original era usar Llama 4 Scout/Maverick via Groq, mas a
 *    própria Groq descontinuou os dois (MODEL_SCOUT/MODEL_MAVERICK) -
 *    passaram a retornar HTTP 404 model_not_found.
 * 2) A tentativa seguinte (MODEL_VISION_DEFAULT = "qwen/qwen3.6-27b",
 *    obtida via WebFetch de console.groq.com/docs/vision) TAMBÉM voltou
 *    HTTP 404 model_not_found no aparelho real (Xiaomi, testado ao
 *    vivo) - ou seja, essa fonte também não é confiável (o nome de
 *    versão "3.6"/"3.8" nem bate com o padrão real de versionamento do
 *    Qwen, então é bem possível que o scraping tenha vindo errado).
 *
 * Como eu (Claude) não consigo bater diretamente em api.groq.com a
 * partir de nenhuma das minhas ferramentas neste ambiente (o proxy de
 * saída bloqueia esse host tanto no meu sandbox quanto na VM sandboxada
 * usada pelo device_bash), a fonte confiável agora é o próprio
 * dispositivo do usuário rodando `GET /v1/models` com a chave real (ver
 * checklist-hackathon.md). Enquanto isso não chega, esta classe tenta
 * uma CADEIA de modelos candidatos (chatWithImagesAutoModel) em vez de
 * um único nome fixo, avançando para o próximo candidato só quando o
 * erro for especificamente "modelo não existe" (HTTP 404/400 com
 * código model_not_found) - qualquer outro erro (rede, auth, rate
 * limit) é repassado imediatamente, sem mascarar o problema real.
 *
 * Este cliente é agnóstico de domínio (não conhece Procedure/ProcedureStep
 * — isso é responsabilidade de com.stepwise.ai.LlamaStepEvaluator, em
 * :mode-hardware). Vive em :platform-android porque só depende de
 * Android/OkHttp/org.json e do BuildConfig da própria API key.
 */
class LlamaVisionClient(
    private val apiKey: String = BuildConfig.GROQ_API_KEY,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {

    /** false quando não há chave (sempre no build release): quem chama deve seguir sem IA remota. */
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    /** Resultado bruto de uma chamada — o parsing do JSON de domínio fica por conta de quem chama. */
    data class LlamaVisionResult(
        val rawText: String,
        val modelUsed: String,
        val latencyMs: Long
    )

    class LlamaVisionException(
        message: String,
        cause: Throwable? = null,
        val httpCode: Int? = null,
        val errorCode: String? = null
    ) : Exception(message, cause) {
        /** true só quando a Groq confirmou que o MODELO em si não existe/não está acessível. */
        val isModelNotFound: Boolean
            get() = errorCode == "model_not_found" || (httpCode == 404 && message?.contains("does not exist") == true)

        /**
         * Texto para tela/voz. [message] pode trazer o corpo da resposta do
         * provedor e fica só no log; nunca mostrar nem falar [message] ao aluno.
         */
        val userMessage: String
            get() = when {
                httpCode == null && cause is IOException -> "Sem conexão com o serviço de IA."
                httpCode == null && errorCode == ERROR_NOT_CONFIGURED -> "A IA na nuvem não está configurada neste aparelho."
                httpCode == 401 || httpCode == 403 -> "A chave da IA foi recusada. Confira a configuração."
                httpCode == 429 -> "Limite de uso da IA atingido. Tente de novo em instantes."
                isModelNotFound -> "O modelo de IA configurado não está disponível."
                httpCode != null && httpCode >= 500 -> "O serviço de IA está indisponível agora."
                else -> "A IA não conseguiu responder agora."
            }
    }

    /**
     * Envia texto + 1 ou mais imagens (JPEG) para o modelo indicado e
     * retorna o texto de resposta bruto (esperado: um JSON conforme o
     * `systemPrompt` pedir — ver LlamaStepEvaluator).
     *
     * @param images cada item é um frame JPEG já codificado (ex.: o frame
     * capturado do stream do Meta DAT, ou o desenho técnico de referência).
     */
    suspend fun chatWithImages(
        model: String,
        systemPrompt: String,
        userText: String,
        images: List<ByteArray>,
        temperature: Double = 0.2,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): LlamaVisionResult {
        if (apiKey.isBlank()) {
            throw LlamaVisionException(
                "GROQ_API_KEY não configurada. Defina groq.api.key em local.properties " +
                    "(ver local.properties.example) ou a variável de ambiente GROQ_API_KEY; " +
                    "só o build debug a embute.",
                errorCode = ERROR_NOT_CONFIGURED
            )
        }

        val contentArray = JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", userText))
            for (image in images) {
                val b64 = Base64.encodeToString(image, Base64.NO_WRAP)
                put(
                    JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$b64"))
                )
            }
        }

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            put(JSONObject().put("role", "user").put("content", contentArray))
        }

        val body = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
        }

        val request = Request.Builder()
            .url(BASE_URL)
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val startedAt = System.currentTimeMillis()
        val responseBody = executeAsync(request)
        val latencyMs = System.currentTimeMillis() - startedAt

        val parsed = try {
            JSONObject(responseBody)
        } catch (e: org.json.JSONException) {
            throw LlamaVisionException("Resposta da Groq não é um JSON válido: ${responseBody.take(200)}", e)
        }

        if (parsed.has("error")) {
            throw LlamaVisionException("Groq retornou erro: ${parsed.getJSONObject("error").optString("message")}")
        }

        val text = parsed.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?: throw LlamaVisionException("Resposta da Groq sem choices[0].message.content: $responseBody")

        Log.i(TAG, "Groq $model respondeu em ${latencyMs}ms")
        return LlamaVisionResult(rawText = text, modelUsed = model, latencyMs = latencyMs)
    }

    /**
     * Igual a [chatWithImages], mas tenta uma CADEIA de modelos candidatos
     * em ordem, só avançando para o próximo quando a Groq confirmar que o
     * modelo atual não existe/não está disponível para esta conta
     * ([LlamaVisionException.isModelNotFound]). Qualquer outro tipo de
     * falha (rede, auth, rate limit, JSON malformado) é repassada na hora,
     * sem tentar o próximo candidato — não faz sentido trocar de modelo
     * para um problema que não é de modelo.
     *
     * Isso existe porque, na prática (18/09/2026), já tivemos DOIS nomes
     * de modelo "confirmados" por fontes de documentação que devolveram
     * HTTP 404 no aparelho real. Enquanto o nome certo não é confirmado
     * direto na conta do usuário (ver checklist-hackathon.md), a cadeia é
     * a rede de segurança para a demo ao vivo não travar em um 404 só.
     */
    suspend fun chatWithImagesAutoModel(
        systemPrompt: String,
        userText: String,
        images: List<ByteArray>,
        temperature: Double = 0.2,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
        models: List<String> = MODEL_FALLBACK_CHAIN
    ): LlamaVisionResult {
        require(models.isNotEmpty()) { "models não pode ser vazio" }
        var lastModelNotFound: LlamaVisionException? = null
        for ((index, candidate) in models.withIndex()) {
            try {
                return chatWithImages(candidate, systemPrompt, userText, images, temperature, maxTokens)
            } catch (e: LlamaVisionException) {
                if (e.isModelNotFound) {
                    Log.w(TAG, "Modelo '$candidate' não existe/não disponível (${index + 1}/${models.size}); tentando próximo candidato.", e)
                    lastModelNotFound = e
                    continue
                }
                throw e // erro que não é "modelo não existe" - não adianta trocar de modelo
            }
        }
        throw lastModelNotFound ?: LlamaVisionException("Nenhum modelo candidato disponível.")
    }

    private suspend fun executeAsync(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = httpClient.newCall(request)
        // Sessão encerrada/tela fechada: aborta a requisição em vez de deixá-la terminar à toa.
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(LlamaVisionException("Falha de rede ao chamar a Groq", e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    // Falha lendo o corpo também precisa retomar a coroutine; senão ela fica presa.
                    val text = try {
                        it.body?.string().orEmpty()
                    } catch (e: IOException) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(LlamaVisionException("Falha de rede ao ler a resposta da Groq", e))
                        }
                        return
                    }
                    if (!continuation.isActive) return
                    if (!it.isSuccessful) {
                        val errorCode = try {
                            JSONObject(text).optJSONObject("error")?.optString("code")
                        } catch (parseError: org.json.JSONException) {
                            null
                        }
                        continuation.resumeWithException(
                            LlamaVisionException(
                                "Groq HTTP ${it.code}: ${text.take(300)}",
                                httpCode = it.code,
                                errorCode = errorCode
                            )
                        )
                        return
                    }
                    continuation.resume(text)
                }
            }
        })
    }

    companion object {
        private const val TAG = "LlamaVisionClient"
        private const val ERROR_NOT_CONFIGURED = "not_configured"
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        // 512 era baixo demais: um plano de CAD com vários passos (JSON
        // estruturado) já estoura isso e o JSON vem cortado no meio (erro
        // "Unterminated array" - achado em produção, 18/09/2026). Chamadas
        // de plano usam um valor maior explicitamente (ver LlamaCadTutorProvider).
        private const val DEFAULT_MAX_TOKENS = 1024
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        // ATENÇÃO (achado em produção, 18/09/2026): a Groq descontinuou os
        // modelos Llama 4 Scout (dep. 17/07/2026) e Maverick (dep. 09/03/2026) -
        // chamadas para eles agora retornam HTTP 404 "model not found". As
        // constantes abaixo ficam só como referência histórica.
        @Deprecated("Descontinuado pela Groq em 17/07/2026 - retorna HTTP 404.")
        const val MODEL_SCOUT = "meta-llama/llama-4-scout-17b-16e-instruct"
        @Deprecated("Descontinuado pela Groq em 09/03/2026 - retorna HTTP 404.")
        const val MODEL_MAVERICK = "meta-llama/llama-4-maverick-17b-128e-instruct"

        // CONFIRMADO 404 no aparelho real (18/09/2026) apesar de constar na
        // documentação (console.groq.com/docs/vision) - mantido só como
        // referência histórica do segundo palpite errado, NÃO USAR sozinho.
        @Deprecated("Confirmado HTTP 404 model_not_found no aparelho real em 18/09/2026, apesar da doc.")
        const val MODEL_VISION_DEFAULT = "qwen/qwen3.6-27b"

        /**
         * CONFIRMADO via GET /v1/models na conta real do usuário
         * (18/09/2026, ver checklist-hackathon.md): "qwen/qwen3.8-27b" é
         * o único modelo do catálogo da conta com plausibilidade de
         * suportar visão (o resto é texto puro - gpt-oss, compound,
         * prompt-guard - ou áudio - whisper, orpheus). "qwen/qwen3.6-27b"
         * NÃO está na lista real (era o erro anterior). Ainda não
         * confirmado em produção que este modelo aceita content type
         * "image_url" - se a Groq rejeitar por não suportar imagem, o
         * erro NÃO será model_not_found (isModelNotFound=false) e vai
         * aparecer direto na tela, sem mascarar, o que já é o diagnóstico
         * que precisamos nesse caso.
         */
        val MODEL_FALLBACK_CHAIN: List<String> = listOf(
            "qwen/qwen3.8-27b"
        )

        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
