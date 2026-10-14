package com.stepwise.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandParserTest {

    @Test
    fun `reconhece o vocabulario principal`() {
        assertEquals(VoiceCommand.VERIFY, VoiceCommandParser.parse("pode verificar agora"))
        assertEquals(VoiceCommand.HELP, VoiceCommandParser.parse("preciso de ajuda"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommandParser.parse("repetir instrucao"))
    }

    @Test
    fun `aceita variacoes curtas, acentos e pontuacao`() {
        assertEquals(VoiceCommand.VERIFY, VoiceCommandParser.parse("Confere, por favor!"))
        assertEquals(VoiceCommand.HELP, VoiceCommandParser.parse("me ajudar"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommandParser.parse("repete novamente"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommandParser.parse("REPETIR instrução"))
    }

    @Test
    fun `rejeita fala sem comando`() {
        assertNull(VoiceCommandParser.parse("texto sem comando"))
        assertNull(VoiceCommandParser.parse(""))
        assertNull(VoiceCommandParser.parse("[unk]"))
    }

    @Test
    fun `todo o vocabulario resolve para um comando`() {
        for (word in VoiceCommandParser.vocabulary) {
            assert(VoiceCommandParser.parse(word) != null) { "sem comando para '$word'" }
        }
    }
}
