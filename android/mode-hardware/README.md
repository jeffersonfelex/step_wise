# Modo hardware — montagem física

Diretório da equipe que trabalha com peças físicas. O código existente foi movido
para cá preservando seus packages Kotlin, sem reimplementar o detector ou motor.

- `src/main/kotlin/com/stepwise/vision`: detectores e resultados de peças.
- `engine`: motor procedural e fading.
- `procedure`: definição e carregamento de procedimentos físicos.
- `session/SessionController.kt`: controlador da montagem.
- `hardware/HardwareTutorActivity.kt`: guia manual aberto pelo seletor de modos.
- `data/db`: checkpoints Room do procedimento físico; esquema preservado.
- `src/main/assets/procedures`: procedimentos empacotados no APK automaticamente.
- `src/test`: testes do motor e composição de detectores.
- `../../ml`: treino/exportação, continua exclusivo da percepção física.

`StepResult` e `VoiceCommand` estão no `:core`, com os mesmos packages de antes.
Captura Meta DAT, áudio e estado de conexão estão em `:platform-android`.
Não adicionar dependência de `:mode-software`.

Da pasta `android/`, com JDK 17 e Android SDK 34:

```bash
./gradlew :mode-hardware:testDebugUnitTest
```

O botão Hardware abre o procedimento de engrenagens carregado pelo `ProcedureLoader`
a partir do JSON. O guia mostra as seis etapas, permite ouvir a instrução com o TTS
do Android, voltar, confirmar manualmente e reiniciar. A posição é preservada em
recriações da tela. A confirmação manual não chama `applyResult(CORRECT)` nem gera
evidência visual ou checkpoint no motor automático.

Os TODOs de captura real, controlador automático e avaliação de classes no motor
continuam pendentes. O guia avisa que a montagem não foi verificada automaticamente.
Mudanças na integração física dos óculos devem ser feitas em `:platform-android`
e coordenadas com a equipe software, que também usa os óculos.
