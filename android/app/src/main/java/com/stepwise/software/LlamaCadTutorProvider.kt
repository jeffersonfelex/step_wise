package com.stepwise.software

import android.util.Log
import com.stepwise.ai.LlamaVisionClient
import com.stepwise.core.GoalAssessment
import com.stepwise.core.TutoringGoal
import com.stepwise.core.TutoringPlan
import com.stepwise.engine.StepResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Implementação real (adapter) de [SoftwareTutorProvider] usando o mesmo
 * modelo Llama 4 com visão (via Groq) já usado no modo hardware — ver
 * com.stepwise.ai.LlamaVisionClient em :platform-android.
 *
 * Fica em :app (não em :mode-software) de propósito: o domínio
 * (SoftwareTutorProvider/Connection/Session, em :mode-software) é
 * "provider-independent" e não pode depender de SDK de nuvem nem de
 * Android (ver KDoc de SoftwareTutorProvider) — só o adapter concreto,
 * que roda na camada de app, conhece a Groq/Llama.
 *
 * Simplificação consciente (tempo do hackathon): os eventos "de texto"
 * não são token-a-token (sem streaming real de tokens da Groq neste
 * modelo de chamada); cada resposta chega como um único TextDelta com o
 * texto completo, o que ainda satisfaz o contrato do Flow<SoftwareTutorEvent>.
 */
class LlamaCadTutorProvider(
    private val visionClient: LlamaVisionClient = LlamaVisionClient()
) : SoftwareTutorProvider {

    override suspend fun connect(task: CadTask): SoftwareTutorConnection =
        LlamaCadTutorConnection(task, visionClient)
}

private class LlamaCadTutorConnection(
    private val task: CadTask,
    private val visionClient: LlamaVisionClient
) : SoftwareTutorConnection {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val eventChannel = Channel<SoftwareTutorEvent>(Channel.BUFFERED)
    private var activeJob: Job? = null

    @Volatile private var currentPlan: TutoringPlan? = null

    override val events: Flow<SoftwareTutorEvent> = eventChannel.receiveAsFlow()

    init {
        // Gera o plano assim que a conexão abre - é a leitura do desenho
        // técnico recebido pelo app (task.drawing), o "passo a passo" pedido.
        activeJob = scope.launch { proposeInitialPlan() }
    }

    override suspend fun send(input: SoftwareTutorInput) {
        when (input) {
            is SoftwareTutorInput.Text -> handleText(input.text)
            is SoftwareTutorInput.Verify -> handleVerify(input.frame.bytes, input.goalId)
            is SoftwareTutorInput.Audio -> {
                // ASR já acontece no app (Vosk, offline) antes de chegar aqui como Text -
                // este provider não recebe PCM bruto.
                eventChannel.send(
                    SoftwareTutorEvent.Failure("Entrada de áudio direta não suportada; envie como Text já transcrito.")
                )
            }
            SoftwareTutorInput.Interrupt -> activeJob?.cancel()
        }
    }

    override suspend fun close() {
        activeJob?.cancel()
        scope.cancel()
        eventChannel.close()
    }

    private suspend fun proposeInitialPlan() {
        try {
            val response = visionClient.chatWithImagesAutoModel(
                systemPrompt = planSystemPrompt(task),
                userText = "Gere o passo a passo para modelar este desenho técnico em " +
                    "${task.applicationName}. Objetivo do aluno: ${task.objective}.",
                images = listOf(task.drawing.bytes),
                // O plano é JSON estruturado com vários passos - o default de
                // 1024 tokens ainda pode cortar planos longos. Achado em
                // produção (18/09/2026): com 512 o JSON vinha truncado
                // ("Unterminated array"). 3072 dá folga para ~15-20 passos.
                maxTokens = 3072
            )
            val plan = parsePlan(response.rawText)
            currentPlan = plan
            eventChannel.send(SoftwareTutorEvent.PlanProposed(plan))
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao gerar o plano a partir do desenho técnico.", e)
            eventChannel.send(SoftwareTutorEvent.Failure(errorMessage(e, "gerar o passo a passo")))
        } finally {
            eventChannel.send(SoftwareTutorEvent.TurnCompleted)
        }
    }

    private fun handleText(text: String) {
        activeJob = scope.launch {
            try {
                val response = visionClient.chatWithImagesAutoModel(
                    systemPrompt = tutorSystemPrompt(task, currentPlan),
                    userText = text,
                    images = listOf(task.drawing.bytes)
                )
                eventChannel.send(SoftwareTutorEvent.TextDelta(response.rawText.trim()))
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao responder pergunta do aluno.", e)
                eventChannel.send(SoftwareTutorEvent.Failure(errorMessage(e, "responder")))
            } finally {
                eventChannel.send(SoftwareTutorEvent.TurnCompleted)
            }
        }
    }

    private fun handleVerify(frameJpeg: ByteArray, goalId: String) {
        activeJob = scope.launch {
            try {
                val goal = currentPlan?.goals?.find { it.id == goalId }
                val response = visionClient.chatWithImagesAutoModel(
                    systemPrompt = verifySystemPrompt(task, goal),
                    userText = "Avalie o objetivo \"${goal?.instruction ?: goalId}\" com base na imagem enviada.",
                    images = listOf(frameJpeg)
                )
                eventChannel.send(SoftwareTutorEvent.Assessment(parseAssessment(goalId, response.rawText)))
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao verificar o progresso do aluno.", e)
                eventChannel.send(SoftwareTutorEvent.Failure(errorMessage(e, "verificar seu progresso")))
            } finally {
                eventChannel.send(SoftwareTutorEvent.TurnCompleted)
            }
        }
    }

    private fun errorMessage(e: Exception, action: String): String =
        "Não consegui $action agora (${e.message ?: e::class.simpleName}). Tente de novo."

    // -------------------------------------------------------------------
    // Prompts
    // -------------------------------------------------------------------

    private fun planSystemPrompt(task: CadTask): String =
        """
        Você é um tutor paciente de modelagem 3D no ${task.applicationName},
        ensinando um aluno iniciante a montar uma peça a partir de um
        desenho técnico de referência (vista com cotas + perspectiva 3D).

        IMPORTANTE - o ${task.applicationName} NÃO funciona por esboço 2D +
        extrusão (como Fusion 360/SolidWorks). Ele funciona por COMPOSIÇÃO
        DE FORMAS PRIMITIVAS 3D prontas (Caixa, Cilindro, Cone, Teto/Roof
        etc.), que o aluno arrasta para a área de trabalho, redimensiona
        (arrastando os identificadores de canto ou digitando
        comprimento/largura/altura/raio no painel da forma) e posiciona
        (arrastando ou digitando X/Y/Z). Para furos, o aluno arrasta uma
        forma no modo "Furo" (transparente/hachurada) sobre a peça sólida
        no lugar exato do furo e agrupa as duas ("Agrupar") para subtrair o
        material. Para unir peças sólidas, agrupa duas formas sólidas. A
        ferramenta "Alinhar" centraliza formas entre si com precisão.

        NUNCA ensine como desenhar a vista técnica 2D em papel ou em
        software de desenho - o desenho enviado é só a REFERÊNCIA DE
        MEDIDAS (diâmetros, raios, distância entre centros, espessura). O
        objetivo é o aluno construir a peça em 3D no ${task.applicationName}
        usando essas medidas, nunca copiar o desenho como está.

        Gere um plano de execução dividido em passos curtos, ordenados e
        acionáveis, como se estivesse ensinando ao vivo, sempre no
        vocabulário real do ${task.applicationName} (nomes de formas e
        ferramentas: Caixa, Cilindro, modo Furo, Agrupar, Alinhar), citando
        as medidas exatas do desenho em cada passo relevante. Use NO MÁXIMO
        10 passos - agrupe operações relacionadas em um único passo se
        precisar, em vez de detalhar demais (a resposta precisa caber no
        limite de tokens e ser curta o bastante para ser falada em voz alta
        passo a passo).

        Responda ESTRITAMENTE em JSON, sem texto fora do JSON, no formato:
        {"plan_id": "<slug curto>", "steps": [{"id": "<slug curto e único>", "instruction": "<passo em português, curto o bastante para ser falado em voz alta>"}, ...]}
        """.trimIndent()

    private fun tutorSystemPrompt(task: CadTask, plan: TutoringPlan?): String {
        val stepsList = plan?.goals?.joinToString("\n") { "- (${it.id}) ${it.instruction}" }
            ?: "(plano ainda não gerado)"
        return """
            Você é um tutor de modelagem 3D no ${task.applicationName}
            respondendo a uma pergunta do aluno durante a montagem de uma
            peça por formas primitivas (Caixa, Cilindro, modo Furo, Agrupar,
            Alinhar) - não por esboço 2D (objetivo: ${task.objective}).
            O plano de passos já combinado com o aluno é:
            $stepsList

            Responda em português, de forma curta e clara, adequada para ser
            falada em voz alta pelos óculos (poucas frases). Se a pergunta for
            sobre um passo específico, refira-se a ele pelo conteúdo, não pelo id.
        """.trimIndent()
    }

    private fun verifySystemPrompt(task: CadTask, goal: TutoringGoal?): String =
        """
        Você é o verificador de progresso de um tutor de modelagem 3D no
        ${task.applicationName}. O aluno está montando uma peça por formas
        primitivas (Caixa, Cilindro, modo Furo, Agrupar, Alinhar) a partir
        de um desenho técnico de referência, e enviou uma imagem do estado
        atual do trabalho dele (tela do ${task.applicationName} ou peça
        impressa/montada, conforme o caso).

        Passo que deveria ter sido concluído: "${goal?.instruction ?: "desconhecido"}".

        Responda ESTRITAMENTE em JSON, sem texto fora do JSON, no formato:
        {"result": "CORRECT" ou "KNOWN_ERROR" ou "INCONCLUSIVE", "feedback": "<frase curta em português, para ser falada em voz alta>"}

        Use "INCONCLUSIVE" se a imagem não deixar claro se o passo foi
        concluído corretamente - nunca responda "CORRECT" por dúvida.
        """.trimIndent()

    // -------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------

    private fun parsePlan(rawText: String): TutoringPlan {
        val json = extractJson(rawText)
        val planId = json.optString("plan_id").ifBlank { "cad_plan_${System.currentTimeMillis()}" }
        val stepsJson = json.optJSONArray("steps") ?: JSONArray()
        if (stepsJson.length() == 0) {
            throw JSONException("Plano sem nenhum passo: $rawText")
        }

        val usedIds = mutableSetOf<String>()
        val goals = (0 until stepsJson.length()).map { index ->
            val stepJson = stepsJson.getJSONObject(index)
            var id = stepJson.optString("id").ifBlank { "step_${index + 1}" }
            if (!usedIds.add(id)) id = "${id}_${index + 1}" // garante unicidade exigida por TutoringPlan
            usedIds.add(id)
            val instruction = stepJson.optString("instruction").ifBlank { "Passo ${index + 1}." }
            TutoringGoal(id = id, instruction = instruction)
        }

        return TutoringPlan(id = planId, goals = goals)
    }

    private fun parseAssessment(goalId: String, rawText: String): GoalAssessment {
        val json = extractJson(rawText)
        val resultStr = json.optString("result", "INCONCLUSIVE").uppercase()
        val result = try {
            StepResult.valueOf(resultStr)
        } catch (e: IllegalArgumentException) {
            StepResult.INCONCLUSIVE
        }
        val feedback = json.optString("feedback").ifBlank { "Verificação concluída." }
        return GoalAssessment(goalId = goalId, result = result, feedback = feedback)
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

    private companion object {
        const val TAG = "LlamaCadTutorProvider"
    }
}
