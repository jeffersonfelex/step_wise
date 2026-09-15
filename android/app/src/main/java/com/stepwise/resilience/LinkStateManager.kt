package com.stepwise.resilience

/** Estados possíveis da conexão com os óculos / sessão do Meta DAT. */
enum class LinkState { CONNECTED, DISCONNECTED, RECONNECTING, ERROR }

/**
 * Resiliência (REC no diagrama): observa LinkState e erros tipados do
 * Meta DAT, tenta reconectar e aciona a restauração + revalidação do
 * estado procedural antes de permitir qualquer novo avanço de etapa.
 */
class LinkStateManager(
    private val onReconnected: () -> Unit
) {

    private var state: LinkState = LinkState.DISCONNECTED

    fun onLinkStateChanged(newState: LinkState) {
        state = newState
        if (newState == LinkState.CONNECTED) {
            // TODO: restaurar último checkpoint (DB) + revalidar antes do avanço (ENG).
            onReconnected()
        }
    }
}
