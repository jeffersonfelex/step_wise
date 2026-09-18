# Modo hardware — montagem física

Diretório da equipe que trabalha com peças físicas. O código existente foi movido
para cá preservando seus packages Kotlin, sem reimplementar o detector ou motor.

- `src/main/kotlin/com/stepwise/vision`: detectores e resultados de peças.
- `engine`: motor procedural e fading.
- `procedure`: definição e carregamento de procedimentos físicos.
- `session/SessionController.kt`: controlador da montagem.
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

Os TODOs existentes continuam pendentes (incluindo captura real e avaliação de
classes no motor). A nova tela do app seleciona o modo, mas não inicia a sessão.
Mudanças na integração física dos óculos devem ser feitas em `:platform-android`
e coordenadas com a equipe software, que também usa os óculos.
