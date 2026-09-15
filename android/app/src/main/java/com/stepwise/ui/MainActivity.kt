package com.stepwise.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Companion UI (ver diagrama: nó UI).
 *
 * Responsabilidades:
 *  - Selecionar o procedure_id (ex.: "montagem_engrenagens").
 *  - Oferecer o botão "Verificar" como fallback ao comando de voz.
 *  - Encaminhar comandos para o SessionController.
 *
 * Não é o canal principal de interação: nos óculos, tudo acontece por
 * áudio (voz + TTS). Esta tela existe apenas como apoio/fallback.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: layout mínimo com seleção de procedimento e botão "Verificar".
        // TODO: obter instância do SessionController e encaminhar eventos.
    }
}
