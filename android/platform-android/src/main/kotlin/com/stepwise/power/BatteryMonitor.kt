package com.stepwise.power

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Instrumentação de bateria para o critério "Eficiência de bateria"
 * (CP2, ver claude/checklist-hackathon.md): a banca mede % a cada 10 min,
 * no celular E nos óculos, durante uso contínuo representativo.
 *
 * Limitação honesta (não inventar número): a bateria dos óculos só é
 * legível via SDK real do Meta Wearables DAT (DeviceSession não expõe
 * isso na integração atual — ver MetaDatSessionManager, ainda com
 * captureBestFrameFromStream() pendente). Enquanto isso não estiver
 * integrado, só o percentual do celular é medido de verdade; o percentual
 * dos óculos deve ser lido manualmente na demonstração (tela dos óculos /
 * app Meta AI) e declarado como tal no pitch — nunca fabricado aqui.
 *
 * Estratégia de economia declarada (evidência exigida pelo CP2):
 *  - stream de câmera do Meta DAT só fica aberto durante a janela de
 *    verificação (~1s, ver STREAM_WINDOW_MILLIS_DEFAULT em
 *    MetaDatSessionManager), não continuamente;
 *  - HFP (voz) só ativo durante a sessão, não o tempo todo do app;
 *  - frases faladas pelo TTS são curtas (ver instructions L1..L4 dos
 *    procedimentos), reduzindo tempo de alto-falante ligado;
 *  - chamada à Groq (LlamaVisionClient) só ocorre por verificação
 *    solicitada por voz, nunca em polling contínuo.
 */
class BatteryMonitor(private val context: Context) {

    /** Percentual atual de bateria do celular (0-100), ou null se indisponível. */
    fun currentPhoneBatteryPercent(): Int? {
        val batteryManager = context.applicationContext
            .getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return level.takeIf { it in 0..100 }
    }

    /**
     * Inicia a amostragem periódica (padrão: a cada 10 min, conforme o
     * CP2) e loga cada leitura em formato fácil de copiar para a
     * evidência do pitch. `onSample` permite também exibir na tela
     * (ex.: HardwareSessionActivity) sem duplicar a lógica de captura.
     */
    fun startPeriodicLogging(
        scope: CoroutineScope,
        intervalMillis: Long = SAMPLE_INTERVAL_MILLIS,
        onSample: (phonePercent: Int?) -> Unit = {}
    ): Job = scope.launch {
        while (isActive) {
            val pct = currentPhoneBatteryPercent()
            Log.i(TAG, "bateria_celular=${pct?.let { "$it%" } ?: "indisponível"} (óculos: ler manualmente)")
            onSample(pct)
            delay(intervalMillis)
        }
    }

    companion object {
        private const val TAG = "StepWiseBattery"
        const val SAMPLE_INTERVAL_MILLIS = 10 * 60 * 1000L // 10 minutos, conforme CP2
    }
}
