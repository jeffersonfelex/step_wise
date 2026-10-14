package com.stepwise

import android.app.Application
import com.stepwise.audio.VoskModelProvider
import com.stepwise.data.db.AppDatabase
import com.stepwise.glasses.MetaWearables

/**
 * Ponto de entrada do processo Android. Inicializa dependências globais
 * (banco Room, modelo de voz offline) fora do caminho crítico de verificação.
 */
class StepWiseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Cria/abre o banco Room cedo (barato: só abre o arquivo .db,
        // não bloqueia leitura/escrita real) para que PrivacyActivity e
        // SessionController já encontrem a instância singleton pronta.
        AppDatabase.getInstance(this)

        // Descompacta e carrega o modelo Vosk (ASR offline, gramática
        // restrita) em background, fora do caminho crítico. O modelo vem
        // embutido no APK (assets/model) — ver scripts/download_vosk_model.sh
        // ou a tarefa Gradle downloadVoskModel; nunca é baixado em runtime.
        VoskModelProvider.preload(this)

        // O SDK Meta DAT exige inicialização antes de qualquer sessão com os óculos.
        MetaWearables.initialize(this)
    }
}
