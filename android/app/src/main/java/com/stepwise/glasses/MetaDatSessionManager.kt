package com.stepwise.glasses

/**
 * Encapsula a DeviceSession do Meta DAT v0.9 (Ray-Ban Meta Gen 2).
 *
 * Regras de bateria (ver POLICIES no diagrama):
 *  - addCamera(...) em 720x1280, 7 fps, apenas durante a janela de verificação.
 *  - 1 capturePhoto() por tentativa.
 *  - camera.stop() imediatamente após a captura.
 *
 * TODO: substituir pelos tipos reais do SDK Meta DAT quando integrado
 * (com.meta.dat.*). Esta classe isola o restante do app da API do SDK.
 */
class MetaDatSessionManager {

    /**
     * Abre a câmera, captura 1 frame sob demanda e encerra a câmera.
     * Retorna o frame capturado ou lança um erro tipado de LinkState
     * (ver LinkStateManager) em caso de falha de sessão/conexão.
     */
    suspend fun capturePhoto(): ByteArray {
        // TODO: DeviceSession.addCamera(width = 720, height = 1280, fps = 7)
        // TODO: stream.start() -> capturePhoto() -> camera.stop()
        throw NotImplementedError("Integração com Meta DAT v0.9 pendente")
    }
}
