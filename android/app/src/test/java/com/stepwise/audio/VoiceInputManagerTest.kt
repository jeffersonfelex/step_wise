package com.stepwise.audio

import com.stepwise.session.VoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInputManagerTest {

    @Test
    fun `parseCommand reconhece vocabulario principal`() {
        assertEquals(VoiceCommand.VERIFY, VoiceInputManager.parseCommand("pode verificar agora"))
        assertEquals(VoiceCommand.HELP, VoiceInputManager.parseCommand("preciso de ajuda"))
        assertEquals(VoiceCommand.REPEAT, VoiceInputManager.parseCommand("repetir instrucao"))
    }

    @Test
    fun `parseCommand aceita variacoes curtas`() {
        assertEquals(VoiceCommand.VERIFY, VoiceInputManager.parseCommand("confere para mim"))
        assertEquals(VoiceCommand.HELP, VoiceInputManager.parseCommand("me ajudar"))
        assertEquals(VoiceCommand.REPEAT, VoiceInputManager.parseCommand("repete novamente"))
    }

    @Test
    fun `parseCommand rejeita fala desconhecida`() {
        assertEquals(VoiceCommand.UNKNOWN, VoiceInputManager.parseCommand("texto sem comando"))
        assertEquals(VoiceCommand.UNKNOWN, VoiceInputManager.parseCommand(""))
    }

    @Test
    fun `janela curta emite comando uma vez e fecha`() {
        val emitted = mutableListOf<VoiceCommand>()
        val manager = VoiceInputManager { emitted += it }

        manager.startListeningWindow()
        assertTrue(manager.isListening())
        manager.handleTranscript("verificar")

        assertEquals(listOf(VoiceCommand.VERIFY), emitted)
        assertFalse(manager.isListening())
    }
}
