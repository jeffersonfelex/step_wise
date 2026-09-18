package com.stepwise.software

import com.stepwise.core.*
import com.stepwise.engine.StepResult
import com.stepwise.software.providers.GeminiLiveCodec
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class GeminiLiveCodecTest {
    @Test fun `verification keeps image goal and attempt in same turn`() {
        val frame = ImageFrame(byteArrayOf(1, 2), "image/png", ImageSource.GLASSES, 0)
        val wire = Json.parseToJsonElement(GeminiLiveCodec.input(SoftwareTutorInput.Verify(frame, "sketch", "attempt-2")))
        val parts = wire.jsonObject.getValue("clientContent").jsonObject.getValue("turns").jsonArray.single()
            .jsonObject.getValue("parts").jsonArray
        assertEquals("image/png", parts[0].jsonObject.getValue("inlineData").jsonObject.getValue("mimeType").jsonPrimitive.content)
        assertTrue(parts[1].toString().contains("sketch"))
        assertTrue(parts[1].toString().contains("attempt-2"))
    }

    @Test fun `decodes all content parts transcript and tool call in one message`() {
        val decoded = GeminiLiveCodec.decode(Json.parseToJsonElement("""
            {"serverContent":{"outputTranscription":{"text":"Confira a cota"},
              "modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm;rate=24000","data":"AAA="}}]},
              "turnComplete":true},
             "toolCall":{"functionCalls":[{"id":"c","name":"report_assessment","args":{
              "goal_id":"sketch","request_id":"r","result":"INCONCLUSIVE","feedback":"Cota ilegível"}}]}}
        """.trimIndent()).jsonObject)
        assertEquals(4, decoded.events.size)
        assertEquals(24_000, (decoded.events[1] as SoftwareTutorEvent.Audio).chunk.sampleRateHz)
        val assessment = decoded.events.last() as SoftwareTutorEvent.Assessment
        assertEquals("r", assessment.requestId)
        assertEquals(StepResult.INCONCLUSIVE, assessment.assessment.result)
        assertNotNull(decoded.response)
    }

    @Test fun `invalid tool result is rejected without emitting approval`() {
        val decoded = GeminiLiveCodec.decode(Json.parseToJsonElement("""
            {"toolCall":{"functionCalls":[{"id":"c","name":"report_assessment","args":{
            "goal_id":"sketch","request_id":"r","result":"PROBABLY_OK","feedback":"Maybe"}}]}}
        """.trimIndent()).jsonObject)
        assertTrue(decoded.events.isEmpty())
        assertTrue(decoded.response!!.contains("inválidos"))
    }

    @Test fun `interruption discards audio carried in same message`() {
        val decoded = GeminiLiveCodec.decode(Json.parseToJsonElement("""
            {"serverContent":{"interrupted":true,"modelTurn":{"parts":[
              {"inlineData":{"mimeType":"audio/pcm;rate=24000","data":"AAA="}}]}}}
        """.trimIndent()).jsonObject)
        assertEquals(listOf(SoftwareTutorEvent.Interrupted), decoded.events)
    }
}
