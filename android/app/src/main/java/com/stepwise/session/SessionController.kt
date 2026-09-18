package com.stepwise.session

import com.stepwise.audio.TtsManager
import com.stepwise.audio.VoiceInputManager
import com.stepwise.engine.ProceduralEngine
import com.stepwise.glasses.MetaDatSessionManager
import com.stepwise.procedure.ProcedureLoader
import com.stepwise.resilience.LinkStateManager

/**
 * Controlador de sessÃ£o (CTRL no diagrama de arquitetura).
 *
 * Orquestra:
 *  - VOICE/UI -> comandos (VERIFY, HELP, REPEAT)
 *  - CTRL -> DAT (VERIFY: dispara captura de 1 frame)
 *  - CTRL -> ENG (HELP/REPEAT: solicita instruÃ§Ã£o sem nova captura)
 *  - CTRL -> PROC (carrega procedure_id ativo)
 *  - REC -> CTRL (reconexÃ£o / restauraÃ§Ã£o de estado)
 */
class SessionController(
    private val metaDatSessionManager: MetaDatSessionManager,
    private val proceduralEngine: ProceduralEngine,
    private val voiceInputManager: VoiceInputManager,
    private val ttsManager: TtsManager,
    private val procedureLoader: ProcedureLoader,
    private val linkStateManager: LinkStateManager
) {

    /** Carrega o procedimento ativo (ex.: "montagem_engrenagens") e restaura estado. */
    fun startSession(procedureId: String) {
        // TODO: procedureLoader.load(procedureId)
        // TODO: restaurar checkpoint via engine + Room (ver ENG <- DB)
        // TODO: registrar listener de comandos (voiceInputManager -> onCommand)
    }

    fun onCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.VERIFY -> handleVerify()
            VoiceCommand.HELP -> handleHelp()
            VoiceCommand.REPEAT -> handleRepeat()
            VoiceCommand.UNKNOWN -> handleUnknown()
        }
    }

    private fun handleVerify() {
        // TODO: responder "Verificando" imediatamente via TTS (evitar silÃªncio).
        // TODO: metaDatSessionManager.capturePhoto() -> QualityGate -> VisualDetector -> engine.evaluate(...)
    }

    private fun handleHelp() {
        // TODO: engine.currentInstruction(helpRequested = true) -> ttsManager.speak(...)
    }

    private fun handleRepeat() {
        // TODO: engine.currentInstruction(repeat = true) -> ttsManager.speak(...)
    }

    private fun handleUnknown() {
        // Comando rejeitado: nao captura imagem, nao altera estado e nao avanca etapa.
    }

    /** Chamado pelo LinkStateManager apÃ³s reconexÃ£o bem-sucedida. */
    fun onReconnected() {
        // TODO: revalidar estado (REC -> ENG: "restaurar + revalidar") antes de permitir novo avanÃ§o.
    }
}
