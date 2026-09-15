package com.stepwise

import android.app.Application

/**
 * Ponto de entrada do processo Android. Inicializa dependências globais
 * (banco Room, gerenciador de TTS) fora do caminho crítico de verificação.
 */
class StepWiseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // TODO: inicializar AppDatabase, TtsManager e demais singletons leves.
    }
}
