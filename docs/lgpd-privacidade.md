# Privacidade e Dados — StepWise (modo hardware)

> Escopo: montagem física. O tutor CAD (modo software) envia desenho, imagens e
> perguntas a provedores remotos com conexão explícita; seu fluxo está em
> [modo-software-primeiro-fluxo.md](modo-software-primeiro-fluxo.md#dados-e-ciclo-de-vida).

*(1 página — preparada para a demonstração de 18/09 e revisada na integração de 10/2026; checkpoint obrigatório de privacidade do Edital §8.1.)*

## Que dados passam pela solução

- **Imagem**: 1 foto da bancada de montagem, capturada pelos óculos sob comando explícito ("verificar"). Não há gravação contínua.
- **Voz**: comando de voz curto ("verificar", "ajuda", "repetir" e variações), captado pelos microfones dos óculos.
- **Não coletamos**: rosto, biometria facial, dados de saúde, localização ou qualquer identificador da pessoa. A câmera classifica *estados da montagem* (ex.: torneira com P2 e P3), não o aprendiz.

## Onde é processado

No smartphone pareado, por padrão:

- o classificador visual (TFLite, `TfliteClassifierDetector`) e as regras do procedimento (`ProceduralEngine`) decidem a etapa localmente;
- o reconhecimento de comando de voz é offline (Vosk com gramática restrita);
- o SDK Meta DAT roda com opt-out de analytics e crash report no manifesto.

**Exceção opcional — avaliação por IA na nuvem.** Quando o build de
desenvolvimento (debug) recebe uma chave da Groq (`groq.api.key`), e somente
quando o modelo local não tem confiança suficiente (resultado `INCONCLUSIVE`),
a foto daquela verificação é enviada à API da Groq para avaliação. A tela da
sessão avisa quando esse modo está ativo. O build release nunca embute a chave,
então nele a verificação é sempre 100% local.

## O que fica armazenado, e por quanto tempo

| Dado | Onde | Retenção |
|---|---|---|
| Foto da verificação | Nunca persistida — vive só na memória, dentro da coroutine de verificação | Descartada após a decisão; a câmera é fechada logo após a captura |
| Áudio do comando | Nunca persistido — processado em memória pelo reconhecedor offline | Descartado após o reconhecimento |
| Eventos mínimos de desempenho (`step_id`, nível de assistência, resultado, se houve pedido de ajuda, timestamp) | Room, localmente no celular do aprendiz (usados para retomar a sessão) | Até o aprendiz apagar na tela **Privacidade e meus dados** (`SessionStateDao.clearAll()`) |

Resultados forçados no modo demonstração não são gravados.

Os eventos de desempenho são **dado pessoal** (art. 5º, X, LGPD). Se um instrutor usar esse histórico para avaliar o aprendiz, essa é uma finalidade diferente da operação do app — o aprendiz precisa poder ver e apagar o próprio histórico a qualquer momento, e ser informado dessa outra finalidade antes que ela aconteça.

## Base legal (LGPD)

- **Art. 7º** — tratamento necessário para a execução do serviço educacional solicitado pelo próprio aprendiz (e/ou consentimento, conforme o modelo de adoção pelo curso).
- **Art. 11 (dados sensíveis)** — não se aplica ao funcionamento central: o sistema não faz reconhecimento facial nem biometria; classifica a montagem, não a identidade da pessoa. Se no futuro qualquer feature passar a identificar o aprendiz (ex.: por voz), essa base precisa ser revisitada.
- **Art. 14 (crianças e adolescentes)** — o usuário-alvo definido é de 18 a 30 anos, mas cursos técnicos e programas de aprendizagem industrial frequentemente têm alunos menores de 18. Se o público se ampliar, o tratamento de dados de adolescentes deve ser feito no melhor interesse deles, com consentimento de responsável quando exigido.

## Quem pode aparecer na câmera sem ter escolhido estar ali

Colegas de bancada ao fundo, no instante em que "verificar" abre a câmera. Mitigação: a câmera só abre sob comando explícito, por uma janela curta, apontada para a bancada (não para o ambiente/sala); a foto é processada e descartada, nunca armazenada. Com a IA na nuvem ativa (só em build de desenvolvimento), essa foto pode ser transmitida — por isso o aviso na tela e a ausência da chave no release.

## O que provedores de nuvem fazem com os dados

No build release, **nenhum dado da montagem sai do dispositivo**. A StepWise trocou o `SpeechRecognizer` padrão do Android (que pode enviar áudio a servidores do Google) por um reconhecedor offline — ver `com.stepwise.audio.KeywordSpotter`. No build de desenvolvimento com chave da Groq, fotos inconclusivas são enviadas à Groq sob os termos dessa API; antes de usar esse modo com aprendizes reais, registrar a finalidade, a base legal e informar quem aparece na bancada.

## "Descartamos o dado" — o trecho de código

```kotlin
// SessionController.handleVerify — o frame vive só dentro desta coroutine:
// nunca vai para um campo, para o disco ou para o Room.
val frame = try {
    withContext(Dispatchers.Default) { metaDatSessionManager.captureBestFrameFromStream() }
} finally {
    metaDatSessionManager.stopStream() // câmera fechada antes do resultado
}
val local = proceduralEngine.evaluateDecision(visualDetector.detect(frame), imageUsable = true)
if (local.result != StepResult.INCONCLUSIVE || cloudEvaluator == null) {
    return@launch finish(step, local.result, local.message) // decisão 100% local
}
```

Código completo em `android/mode-hardware/src/main/kotlin/com/stepwise/session/SessionController.kt`.
