package com.stepwise.software

import com.stepwise.core.ImageFrame
import com.stepwise.core.ImageSource
import com.stepwise.software.providers.GeminiLiveProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class GeminiLiveProviderTest {
    private val task = CadTask(ImageFrame(byteArrayOf(1), "image/jpeg", ImageSource.IMPORTED_DRAWING, 0), "Modelar suporte")

    @Test fun `waits for setup then sends drawing and receives plan before collector starts`() = runBlocking {
        val messages = LinkedBlockingQueue<String>()
        val server = MockWebServer()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                messages.add(text)
                if (text.contains("\"setup\"")) webSocket.send("{\"setupComplete\":{}}")
                if (text.contains("Desenho de referência")) webSocket.send("""
                    {"toolCall":{"functionCalls":[{"id":"call1","name":"propose_plan","args":{
                    "id":"support","goals":[{"id":"sketch","instruction":"Crie o perfil"}]}}]}}
                """.trimIndent())
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, null) }
        }))
        server.start()
        try {
            val provider = GeminiLiveProvider("temporary-test-token").apply { endpoint = server.url("/live").toString() }
            val connection = withTimeout(5000) { provider.connect(task) }
            try {
                val event = withTimeout(5000) { connection.events.first() } as SoftwareTutorEvent.PlanProposed
                assertEquals("sketch", event.plan.goals.single().id)
                assertTrue(messages.poll(2, TimeUnit.SECONDS)!!.contains("generationConfig"))
                assertTrue(messages.poll(2, TimeUnit.SECONDS)!!.contains("inlineData"))
                assertTrue(messages.poll(2, TimeUnit.SECONDS)!!.contains("functionResponses"))
                assertEquals("Token temporary-test-token", server.takeRequest().getHeader("Authorization"))
            } finally { connection.close() }
        } finally { server.shutdown() }
    }

    @Test fun `connection errors never expose authentication or response body`() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401).setBody("private-provider-details"))
        server.start()
        try {
            val provider = GeminiLiveProvider("secret-token").apply { endpoint = server.url("/live").toString() }
            try {
                withTimeout(5000) { provider.connect(task) }
                fail("Expected connection failure")
            } catch (error: java.io.IOException) {
                assertFalse(error.message.orEmpty().contains("secret-token"))
                assertFalse(error.message.orEmpty().contains("private-provider-details"))
            }
        } finally { server.shutdown() }
    }

    @Test fun `setup deadline is an operational error not user cancellation`() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {}))
        server.start()
        try {
            val provider = GeminiLiveProvider("test-token").apply {
                endpoint = server.url("/live").toString()
                setupTimeoutMillis = 300
            }
            try {
                withTimeout(5000) { provider.connect(task) }
                fail("Expected setup deadline")
            } catch (error: TutorConnectionException) {
                assertTrue(error.message.orEmpty().contains("demorou"))
            }
        } finally { server.shutdown() }
    }

    @Test fun `user cancellation while waiting for setup remains cancellation`() = runBlocking {
        val setupReceived = CompletableDeferred<Unit>()
        val server = MockWebServer()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) { setupReceived.complete(Unit) }
        }))
        server.start()
        try {
            val provider = GeminiLiveProvider("test-token").apply { endpoint = server.url("/live").toString() }
            val attempt = async { provider.connect(task) }
            withTimeout(5000) { setupReceived.await() }
            attempt.cancelAndJoin()
            assertTrue(attempt.isCancelled)
        } finally { server.shutdown() }
    }

    @Test fun `setup error reports quota without exposing provider details`() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                webSocket.send("""{"error":{"code":429,"message":"private-provider-details"}}""")
            }
        }))
        server.start()
        try {
            val provider = GeminiLiveProvider("test-token").apply { endpoint = server.url("/live").toString() }
            try {
                withTimeout(5000) { provider.connect(task) }
                fail("Expected quota error")
            } catch (error: TutorConnectionException) {
                assertTrue(error.message.orEmpty().contains("cota"))
                assertFalse(error.message.orEmpty().contains("private-provider-details"))
            }
        } finally { server.shutdown() }
    }

    @Test fun `policy close during setup reports code without exposing close reason`() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                webSocket.close(1008, "private-token-details")
            }
        }))
        server.start()
        try {
            val provider = GeminiLiveProvider("test-token").apply { endpoint = server.url("/live").toString() }
            try {
                withTimeout(5000) { provider.connect(task) }
                fail("Expected policy error")
            } catch (error: TutorConnectionException) {
                assertTrue(error.message.orEmpty().contains("1008"))
                assertFalse(error.message.orEmpty().contains("private-token-details"))
            }
        } finally { server.shutdown() }
    }
}
