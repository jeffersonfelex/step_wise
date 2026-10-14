# Modo hardware — montagem física

Diretório da equipe que trabalha com peças físicas. O kit atual é a montagem de
torneira com conexões (peças `P1`–`P6`); ver
[dataset real](../../docs/final-2026-09/dataset-real-stepwise.md).

- `src/main/kotlin/com/stepwise/vision`: detectores. `TfliteClassifierDetector`
  (plano A, classificador do `ml/`) e os planos B determinísticos
  (`ColorSegmentationDetector`, `ArucoMarkerDetector`, `CompositeDetector`).
- `engine`: motor procedural (`evaluateDecision` com limiar de confiança e abstenção) e fading.
- `procedure`: definição e carregamento de procedimentos (`ProcedureLoader`).
- `session/SessionController.kt`: verificação, voz, checkpoints e modo demonstração.
- `ai/LlamaStepEvaluator.kt`: avaliação opcional na nuvem (Groq) quando o motor se abstém.
- `hardware/HardwareTutorActivity.kt`: guia manual (sem óculos), aberto pelo seletor de modos.
- `data/db`: checkpoints Room do procedimento físico.
- `src/main/assets/procedures`: `montagem_torneira_stepwise_v1` (kit real) e
  `montagem_engrenagens` (exemplo antigo; classes compatíveis com o `ColorSegmentationDetector`).
- `src/main/assets/models`: classificador v1 (1,7 MB), rótulos e manifest.
- `src/test`: motor, detectores compostos e validação dos procedimentos contra os rótulos do modelo.
- `src/androidTest`: Room e inferência do classificador (exigem aparelho/emulador).

`StepResult`, `VoiceCommand` e `VoiceCommandParser` estão no `:core`. Captura Meta
DAT, áudio, Vosk e o cliente Groq estão em `:platform-android`. A tela de sessão
(`HardwareSessionActivity`) e a de privacidade ficam no `:app`.
Não adicionar dependência de `:mode-software`.

## Como a verificação decide

1. "Verificar" (voz ou botão) captura uma foto dos óculos e fecha a câmera.
2. O classificador local devolve as classes mais prováveis; o motor só aceita
   `CORRECT`/`KNOWN_ERROR` com confiança ≥ 0,85, senão `INCONCLUSIVE`.
3. Se o motor se abstém **e** há chave Groq (só build debug), a foto vai para o
   `LlamaStepEvaluator`; a tela avisa quando isso está ativo.
4. O resultado é falado, aplicado ao fading e gravado no Room (exceto no modo
   demonstração); a próxima sessão retoma do último checkpoint.

O classificador v1 teve 16% de acurácia no teste: na prática a decisão local fica
em `INCONCLUSIVE` até o modelo ser retreinado (ver [ml/README.md](../../ml/README.md)).

## Testes

Da pasta `android/`, com JDK 17 e Android SDK 36:

```bash
./gradlew :mode-hardware:testDebugUnitTest
./gradlew :mode-hardware:connectedDebugAndroidTest   # com aparelho conectado
```

O guia manual mostra as seis etapas, permite ouvir a instrução com o TTS do
Android, voltar, confirmar e reiniciar. A confirmação manual não gera evidência
visual nem checkpoint no motor automático.
