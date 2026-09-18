package com.stepwise.software.providers

import com.stepwise.software.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import okhttp3.*
import okio.ByteString

/** Uses an ephemeral token supplied at runtime, never an API key embedded in the APK. */
class GeminiLiveProvider(
    private val token: String,
    private val model: String = "gemini-3.8-live",
) : SoftwareTutorProvider {
    internal var endpoint = "https://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained"
    internal var setupTimeoutMillis = 20_000L

    override suspend fun connect(task: CadTask): SoftwareTutorConnection {
        require(token.isNotBlank()) { "Informe um token temporário" }
        val client = OkHttpClient.Builder().build()
        val connection = Connection(client, model)
        val request = Request.Builder().url(endpoint).header("Authorization", "Token $token").build()
        try {
            connection.socket = client.newWebSocket(request, connection)
            // Only our deadline becomes an operational failure. Cancellation by the
            // Activity/parent coroutine must still propagate as cancellation.
            withTimeoutOrNull(setupTimeoutMillis) { connection.ready.await() }
                ?: throw TutorConnectionException("O tutor demorou para conectar. Confira a rede e tente com um novo token.")
            connection.write(GeminiLiveCodec.initial(task))
            return connection
        } catch (error: Throwable) {
            connection.close()
            throw error
        }
    }

    private class Connection(private val client: OkHttpClient, private val model: String) :
        WebSocketListener(), SoftwareTutorConnection {
        lateinit var socket: WebSocket
        val ready = CompletableDeferred<Unit>()
        private val queue = Channel<SoftwareTutorEvent>(256)
        override val events = queue.receiveAsFlow()
        @Volatile private var closed = false

        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.send(GeminiLiveCodec.setup(model))
        }
        override fun onMessage(webSocket: WebSocket, text: String) = message(webSocket, text)
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) = message(webSocket, bytes.utf8())

        private fun message(webSocket: WebSocket, text: String) {
            if (closed) return
            try {
                val message = Json.parseToJsonElement(text).jsonObject
                if ("error" in message) {
                    val error = message["error"] as? JsonObject
                    fail(serviceError((error?.get("code") as? JsonPrimitive)?.intOrNull))
                    return
                }
                if ("setupComplete" in message) ready.complete(Unit)
                val decoded = GeminiLiveCodec.decode(message)
                for (event in decoded.events) {
                    if (!queue.trySend(event).isSuccess) {
                        fail("A reprodução não acompanhou o fluxo. Inicie outra sessão.")
                        return
                    }
                }
                decoded.response?.let { if (!webSocket.send(it)) fail("Falha ao responder ao tutor") }
            } catch (_: Exception) {
                fail("Resposta inválida do provedor. Inicie outra sessão.")
            }
        }

        fun write(message: String) {
            check(!closed && socket.send(message)) { "Conexão do tutor encerrada" }
        }
        override suspend fun send(input: SoftwareTutorInput) { write(GeminiLiveCodec.input(input)) }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // Do not expose URLs, headers, credentials, or provider error bodies.
            fail(serviceError(response?.code))
        }
        private fun serviceError(code: Int?): String = when (code) {
                400 -> "O Gemini recusou a configuração da sessão (400). Confira o nome e a compatibilidade do modelo Live."
                401, 403 -> "Acesso recusado. Confira o token temporário e o acesso da conta ao modelo."
                404 -> "Modelo ou serviço não encontrado (404). Confira o nome do modelo Live e o acesso da conta."
                429 -> "Limite de uso do serviço atingido. Confira a cota da conta antes de tentar novamente."
                500, 502, 503, 504 -> "O serviço Gemini está indisponível ($code). Tente novamente mais tarde com um novo token."
                else -> "Não foi possível manter a conexão com o tutor. Verifique rede e token."
        }
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, null)
            fail(when (code) {
                1007 -> "O Gemini recusou o formato de uma mensagem (1007). É necessário revisar a integração do aplicativo."
                1008 -> "O Gemini recusou a sessão (1008). Confira modelo, acesso da conta e use um token novo."
                1011 -> "O Gemini encerrou a sessão por erro interno (1011). Tente novamente com um token novo."
                else -> "Sessão remota encerrada ($code). Conecte novamente com um novo token."
            })
        }
        private fun fail(message: String) {
            if (closed) return
            ready.completeExceptionally(TutorConnectionException(message))
            queue.trySend(SoftwareTutorEvent.Failure(message))
            release(TutorConnectionException(message))
        }
        @Synchronized private fun release(error: Throwable? = null) {
            if (closed) return
            closed = true
            if (::socket.isInitialized) socket.cancel()
            queue.close(error)
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
        override suspend fun close() { release() }
    }
}
