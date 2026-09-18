# Privacidade e Dados — StepWise (modo hardware)

> Documento do escopo físico original; não descreve o futuro tutor CAD nem
> provedores remotos. Há fluxos planejados aqui que ainda estão pendentes no código.

*(1 página — preparada para a demonstração de 18/09; checkpoint obrigatório de privacidade do Edital §8.1.)*

## Que dados passam pela solução

- **Imagem**: 1 frame de vídeo da bancada de montagem, capturado sob comando explícito ("verificar"). Não há gravação contínua.
- **Voz**: comando de voz curto ("verificar", "ajuda", "repetir"), captado pelos microfones dos óculos.
- **Não coletamos**: rosto, biometria facial, dados de saúde, localização ou qualquer identificador da pessoa. A câmera detecta *peças da montagem* (classes de objeto — engrenagem azul, eixo principal etc.), não o aprendiz.

## Onde é processado

100% no smartphone pareado. O detector visual (MediaPipe/LiteRT ou o plano B determinístico) e o reconhecimento de comando de voz (KWS offline) rodam localmente — nenhum frame ou áudio é enviado a servidor ou nuvem no caminho crítico (ver `docs/architecture-stepwise.mmd`, subgraph `ANDROID`: "sem cloud no caminho crítico").

## O que fica armazenado, e por quanto tempo

| Dado | Onde | Retenção |
|---|---|---|
| Frame de imagem | Nunca persistido — vive só na memória, dentro da função que o captura | Descartado imediatamente após a inferência |
| Áudio do comando | Nunca persistido — processado em memória pelo KWS offline | Descartado após o reconhecimento |
| Eventos mínimos de desempenho (`step_id`, nível de assistência, resultado, se houve pedido de ajuda, timestamp) | Room, localmente no celular do aprendiz | Até o aprendiz apagar — ver `SessionStateDao.clear(procedureId)` |

Os eventos de desempenho são **dado pessoal** (art. 5º, X, LGPD). Se um instrutor usar esse histórico para avaliar o aprendiz, essa é uma finalidade diferente da operação do app — o aprendiz precisa poder ver e apagar o próprio histórico a qualquer momento, e ser informado dessa outra finalidade antes que ela aconteça.

## Base legal (LGPD)

- **Art. 7º** — tratamento necessário para a execução do serviço educacional solicitado pelo próprio aprendiz (e/ou consentimento, conforme o modelo de adoção pelo curso).
- **Art. 11 (dados sensíveis)** — não se aplica ao funcionamento central: o sistema não faz reconhecimento facial nem biometria; detecta classes de peças, não a identidade da pessoa. Se no futuro qualquer feature passar a identificar o aprendiz (ex.: por voz), essa base precisa ser revisitada.
- **Art. 14 (crianças e adolescentes)** — o usuário-alvo definido é de 18 a 30 anos, mas cursos técnicos e programas de aprendizagem industrial frequentemente têm alunos menores de 18. Se o público se ampliar, o tratamento de dados de adolescentes deve ser feito no melhor interesse deles, com consentimento de responsável quando exigido.

## Quem pode aparecer na câmera sem ter escolhido estar ali

Colegas de bancada ao fundo, no instante em que "verificar" abre a janela de câmera. Mitigação: a câmera só abre sob comando explícito, por uma janela curta, apontada para a bancada (não para o ambiente/sala); o frame é processado e descartado, nunca armazenado ou transmitido. **Vale dizer isso explicitamente no pitch** — é uma boa decisão de privacidade, não só uma limitação técnica.

## O que provedores de nuvem fazem com os dados

**Nenhum dado sai do dispositivo no caminho crítico** — não há provedor de nuvem envolvido na captura, no processamento ou no armazenamento. É por isso que a StepWise trocou o `SpeechRecognizer` padrão do Android (que pode enviar áudio a servidores do Google) por um detector de palavras-chave offline — ver `com.stepwise.audio.KeywordSpotter` no código. Se no futuro houver telemetria opcional (ex.: logs de erro de app), isso deve ser declarado aqui separadamente, com opt-in explícito.

## "Descartamos o dado" — o trecho de código

```kotlin
// SessionController.kt — o frame capturado vive só dentro desta coroutine:
// nunca é atribuído a um campo da classe, nunca é escrito em disco, e
// sai de escopo (liberado pelo garbage collector) assim que a função
// termina. A câmera é encerrada (stopStream) antes mesmo de sabermos o
// resultado da verificação.
private fun handleVerify() {
    ttsManager.speak("Verificando")

    scope.launch {
        val frame = metaDatSessionManager.captureBestFrameFromStream()
        metaDatSessionManager.stopStream() // câmera encerrada já aqui

        if (frame == null) {
            applyAndSpeak(StepResult.INCONCLUSIVE)
            return@launch
        }

        val detection = visualDetector.detect(frame)
        val result = proceduralEngine.evaluate(detection, imageUsable = true)
        applyAndSpeak(result)
        // `frame` sai de escopo aqui: nunca foi persistido, nunca saiu do dispositivo.
    }
}
```

Código completo em `android/mode-hardware/src/main/kotlin/com/stepwise/session/SessionController.kt`.
