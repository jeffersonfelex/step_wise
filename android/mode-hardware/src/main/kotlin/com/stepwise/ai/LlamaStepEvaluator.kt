package com.stepwise.ai

import android.content.Context
import android.util.Log
import com.stepwise.engine.StepResult
import com.stepwise.procedure.ProcedureStep
import java.io.IOException
import org.json.JSONException
import org.json.JSONObject

/** Resultado estruturado de uma avaliação por IA de uma etapa. */
data class StepEvaluation(
    val result: StepResult,
    val feedback: String,
    val confidence: Float,
    val latencyMs: Long,
    val modelUsed: String
)

/**
 * Avaliador de etapa via IA real (critério "Uso de IA", ver
 * claude/checklist-hackathon.md): compara o frame ao vivo capturado dos
 * óculos com o desenho técnico de referência da montagem, usando um
 * modelo Llama 4 com visão (Scout/Maverick, via Groq — ver
 * com.stepwise.ai.LlamaVisionClient em :platform-android) para decidir
 * CORRECT / KNOWN_ERROR / INCONCLUSIVE.
 *
 * Este é o "melhor fluxograma" pedido: entrada externa (imagem real) muda
 * a saída (resultado + feedback falado), o que é exatamente o que a
 * régua de evidência do hackathon exige e o que falta hoje no
 * ProceduralEngine.evaluate() (determinístico, sem IA no caminho
 * crítico — ver KDoc de ProceduralEngine).
 *
 * O desenho técnico de referência é opcional em tempo de execução: se
 * `assets/procedures/<procedureId>_desenho.jpg` não existir neste
 * checkout, a avaliação continua (usando só o frame ao vivo), com um
 * aviso no log — nunca falha silenciosamente fingindo ter comparado.
 */
class LlamaStepEvaluator(
    context: Context,
    private val procedureId: String = DEFAULT_PROCEDURE_ID,
    private val visionClient: LlamaVisionClient = LlamaVisionClient()
) {

    private val appContext = context.applicationContext
    private val referenceDrawing: ByteArray? by lazy { loadReferenceDrawing() }

    private fun loadReferenceDrawing(): ByteArray? {
        val assetPath = "procedures/${procedureId}_desenho.jpg"
        return try {
            appContext.assets.open(assetPath).use { it.readBytes() }
        } catch (e: IOException) {
            Log.w(
                TAG,
                "Desenho técnico de referência não encontrado em assets/$assetPath — " +
                    "avaliação por IA vai comparar só o frame ao vivo (sem referência) até " +
                    "o arquivo ser adicionado ao checkout.",
                e
            )
            null
        }
    }

    /**
     * Envia o frame JPEG capturado (ver MetaDatSessionManager.captureBestFrameFromStream)
     * e o desenho de referência (se disponível) para o modelo, junto com
     * as classes esperadas e os erros conhecidos da etapa atual.
     */
    suspend fun evaluate(frameJpeg: ByteArray, step: ProcedureStep): StepEvaluation {
        val drawing = referenceDrawing
        val images = if (drawing != null) listOf(drawing, frameJpeg) else listOf(frameJpeg)

        val expectedList = step.expectedClasses.joinToString(", ")
        val knownErrorsList = if (step.knownErrors.isEmpty()) {
            "nenhum erro conhecido catalogado para esta etapa"
        } else {
            step.knownErrors.joinToString("; ") { "${it.errorId}: ${it.message}" }
        }

        val systemPrompt = buildSystemPrompt(step, expectedList, knownErrorsList, hasDrawing = drawing != null)
        val userText = "Avalie a etapa \"${step.name}\" (step_id=${step.stepId})."

        return try {
            val response = visionClient.chatWithImagesAutoModel(
                systemPrompt = systemPrompt,
                userText = userText,
                images = images
            )
            parseResponse(response)
        } catch (e: Exception) {
            // Cobre falha de rede/HTTP (LlamaVisionClient.LlamaVisionException) e
            // resposta malformada (JSONException) com o mesmo fallback seguro:
            // nunca travar a sessão nem inventar um CORRECT.
            Log.w(TAG, "Falha na avaliação via Groq/Llama; tratando como INCONCLUSIVE.", e)
            StepEvaluation(
                result = StepResult.INCONCLUSIVE,
                feedback = "Não consegui verificar agora. Pode tentar de novo?",
                confidence = 0f,
                latencyMs = 0L,
                modelUsed = "desconhecido (todos os candidatos falharam)"
            )
        }
    }

    private fun buildSystemPrompt(
        step: ProcedureStep,
        expectedList: String,
        knownErrorsList: String,
        hasDrawing: Boolean
    ): String {
        val drawingNote = if (hasDrawing) {
            "A primeira imagem é o desenho técnico de referência da montagem completa. " +
                "A segunda imagem é o frame ao vivo, capturado agora pela câmera dos óculos, " +
                "mostrando o estado atual da montagem do aluno."
        } else {
            "Nenhum desenho técnico de referência foi fornecido para este procedimento; " +
                "avalie apenas com base no frame ao vivo (única imagem) e nas classes esperadas abaixo."
        }
        return """
            Você é o verificador visual de uma etapa de montagem mecânica, parte do tutor StepWise.
            $drawingNote

            Etapa atual: "${step.name}".
            Peças/classes esperadas nesta etapa: $expectedList.
            Erros conhecidos catalogados para esta etapa (use o error_id exato se identificar algum deles): $knownErrorsList.

            Responda ESTRITAMENTE em JSON, sem nenhum texto fora do JSON, no formato:
            {"result": "CORRECT" ou "KNOWN_ERROR" ou "INCONCLUSIVE", "error_id": "<um dos error_id acima, ou null>", "feedback": "<uma frase curta em português, para ser falada em voz alta>", "confidence": <número de 0.0 a 1.0>}

            Regras:
            - Use "CORRECT" só se as peças esperadas estiverem visíveis e corretamente posicionadas (compare com o desenho de referência quando disponível).
            - Use "KNOWN_ERROR" só se o erro corresponder a um dos error_id catalogados acima; nunca invente um error_id novo.
            - Use "INCONCLUSIVE" se a imagem estiver borrada, incompleta, mal enquadrada, ou você não tiver certeza suficiente.
            - Nunca responda "CORRECT" por dúvida; prefira "INCONCLUSIVE".
        """.trimIndent()
    }

    private fun parseResponse(response: LlamaVisionClient.LlamaVisionResult): StepEvaluation {
        val json = extractJson(response.rawText)
        val resultStr = json.optString("result", "INCONCLUSIVE").uppercase()
        val result = try {
            StepResult.valueOf(resultStr)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Groq retornou result inesperado ('$resultStr'); tratando como INCONCLUSIVE.")
            StepResult.INCONCLUSIVE
        }
        val feedback = json.optString("feedback").ifBlank { "Verificação concluída." }
        val confidence = json.optDouble("confidence", 0.0).toFloat().coerceIn(0f, 1f)

        return StepEvaluation(
            result = result,
            feedback = feedback,
            confidence = confidence,
            latencyMs = response.latencyMs,
            modelUsed = response.modelUsed
        )
    }

    /** O modelo às vezes envolve o JSON em markdown (```json ... ```); extrai só o objeto JSON. */
    private fun extractJson(rawText: String): JSONObject {
        val start = rawText.indexOf('{')
        val end = rawText.lastIndexOf('}')
        if (start == -1 || end == -1 || end < start) {
            throw JSONException("Resposta sem objeto JSON reconhecível: ${rawText.take(200)}")
        }
        return JSONObject(rawText.substring(start, end + 1))
    }

    companion object {
        private const val TAG = "LlamaStepEvaluator"
        const val DEFAULT_PROCEDURE_ID = "montagem_engrenagens"
    }
}
