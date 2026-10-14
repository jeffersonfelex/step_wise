package com.stepwise.hardware

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.stepwise.procedure.Procedure
import com.stepwise.procedure.ProcedureLoader
import java.util.Locale

/** Manual guide: confirmation by the learner is never treated as visual evidence. */
class HardwareTutorActivity : Activity() {
    private lateinit var procedure: Procedure
    private var index = 0
    private var completed = false
    private var speech: TextToSpeech? = null
    private lateinit var stepText: TextView
    private lateinit var instruction: TextView
    private lateinit var status: TextView
    private lateinit var previous: Button
    private lateinit var next: Button
    private lateinit var listen: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        fun label(value: String) = TextView(this).apply {
            text = value; textSize = 18f; content.addView(this)
        }
        fun button(value: String, action: () -> Unit) = Button(this).apply {
            text = value; setOnClickListener { action() }; content.addView(this)
        }
        button("Voltar aos modos") { finish() }
        label("Hardware · Montagem física").textSize = 25f
        setContentView(ScrollView(this).apply { addView(content) })
        try {
            procedure = ProcedureLoader(this).load("montagem_torneira_stepwise_v1")
        } catch (_: Exception) {
            label("Não foi possível carregar o procedimento de montagem.")
            return
        }
        index = (savedInstanceState?.getInt("step_index") ?: 0).coerceIn(procedure.steps.indices)
        completed = savedInstanceState?.getBoolean("completed") ?: false
        label(procedure.title)
        label("Guia manual: leia ou ouça cada instrução e confirme a etapa. " +
            "A captura pelos óculos e a verificação automática da montagem ainda não estão disponíveis.")
        stepText = label("")
        instruction = label("")
        status = label("")
        listen = button("Ouvir instrução") {
            if (speech?.speak(instruction.text.toString(), TextToSpeech.QUEUE_FLUSH, null, "hardware-step") == TextToSpeech.ERROR)
                status.text = "Não foi possível reproduzir. A instrução está disponível na tela."
        }.apply { isEnabled = false }
        previous = button("Etapa anterior") {
            speech?.stop()
            if (completed) completed = false else if (index > 0) index--
            render()
        }
        next = button("Concluí esta etapa") {
            speech?.stop()
            if (index == procedure.steps.lastIndex) completed = true else index++
            render()
        }
        button("Reiniciar guia") {
            speech?.stop(); index = 0; completed = false; render()
        }
        render()
        speech = TextToSpeech(this) { result ->
            if (!isDestroyed) {
                val language = if (result == TextToSpeech.SUCCESS) speech?.setLanguage(Locale.forLanguageTag("pt-BR")) else null
                listen.isEnabled = language != null && language >= TextToSpeech.LANG_AVAILABLE
                if (!listen.isEnabled) status.text = "Voz em português indisponível. Use as instruções na tela."
            }
        }
    }

    private fun render() {
        val step = procedure.steps[index]
        stepText.text = if (completed) "Guia concluído" else "Etapa ${index + 1} de ${procedure.steps.size} · ${step.name}"
        instruction.text = if (completed) "Você confirmou todas as etapas de ${procedure.title}."
            else step.instructionsByLevel["L4"] ?: step.name
        status.text = "Progresso confirmado por você; montagem não verificada automaticamente."
        previous.isEnabled = completed || index > 0
        next.isEnabled = !completed
        next.text = if (index == procedure.steps.lastIndex) "Concluir guia" else "Concluí esta etapa"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("step_index", index)
        outState.putBoolean("completed", completed)
        super.onSaveInstanceState(outState)
    }
    override fun onStop() { speech?.stop(); super.onStop() }
    override fun onDestroy() { speech?.shutdown(); speech = null; super.onDestroy() }
}
