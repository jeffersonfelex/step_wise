# StepWise

StepWise e um tutor adaptativo para montagem pratica. O app guia o aprendiz por voz, recebe uma evidencia visual da etapa atual e decide de forma conservadora se a etapa esta correta, se ha um erro conhecido ou se a evidencia e inconclusiva.

Este repositorio esta sendo preparado para a final do programa AI Glasses Brasil. O projeto principal e um app Android/Kotlin que roda no celular companion e, no evento, deve trocar as fontes locais de imagem pela fonte dos oculos Ray-Ban Meta via Meta DAT.

## Estado atual

Validado no POCO Android 16:

- App Android compila, instala e abre.
- TTS em portugues funciona na demo.
- Captura PCM do microfone do celular funciona.
- Parser de comandos `verificar`, `ajuda`, `repetir` funciona em testes e botoes simulados.
- Inferencia TFLite local roda no app.
- Demo controlada roda com estas fontes visuais:
  - imagens fixas dos assets;
  - foto escolhida pelo seletor nativo do Android;
  - foto capturada pela camera do celular.
- Todas as fontes passam pelo mesmo ciclo:

```text
imagem -> TFLite -> motor procedural conservador -> UI -> TTS
```

Limite importante: o modelo visual atual prova o pipeline, mas ainda tem baixa qualidade. Ele nao deve ser usado para avanco automatico sem decisao conservadora ou novo ciclo de treino.

## Estrutura do repositorio

```text
step_wise/
  android/                         App Android/Kotlin
  android/app/src/main/assets/     Modelos, procedimentos e imagens de diagnostico
  dataset stepwise/                Dataset real fotografado do kit da torneira/conexoes
  docs/                            Plano, ambiente validado e registros da preparacao
  ml/                              Treino, avaliacao e artefatos de modelo
  scripts/                         Scripts auxiliares de dataset/diagnostico
```

Documentos principais:

- `docs/PLANO_ACAO_FINAL_STEPWISE.md`: roadmap detalhado da final.
- `docs/ambiente-validado.md`: ambiente, versoes e validacoes executadas.
- `docs/dataset-real-stepwise.md`: dataset, splits, modelo e metricas.
- `docs/meta-ai-registro-real.md`: roteiro para pareamento/registro real dos oculos.
- `docs/cameraaccess-validacao.md`: validacao do exemplo oficial CameraAccess/MDK.

## Requisitos

### Notebook Windows

Validado com:

- Windows 11.
- Android Studio instalado.
- Android SDK 36 e Build Tools 36.0.0.
- JDK 17.
- Gradle wrapper do projeto.
- Platform-tools/ADB.

No workspace atual, alguns binarios locais foram usados em `.tools/`:

```text
.tools/android/platform-tools/adb.exe
.tools/java/jdk-17.0.20.1+1
.tools/gradle-cache
.tools/android-user
```

Em uma maquina nova, voce pode usar Android Studio/JDK instalados globalmente, desde que o projeto compile com JDK 17.

### Celular Android

Validado com POCO Android 16:

- Depuracao USB ativada.
- Instalacao via USB ativada.
- Permissoes do app concedidas quando solicitadas:
  - microfone;
  - camera;
  - dispositivos proximos/Bluetooth.

### Linux/WSL para treino

O treino foi validado em Ubuntu WSL2 com Python 3.10 e TensorFlow 2.15.1. O app Android pode ser compilado no Windows; o Linux e necessario apenas para reproduzir treino/avaliacao com o ambiente usado.

## Rodar o app Android

A partir da raiz do repositorio no PowerShell:

```powershell
$env:GRADLE_USER_HOME='D:\Documents\GitHub\step_wise\.tools\gradle-cache'
$env:ANDROID_USER_HOME='D:\Documents\GitHub\step_wise\.tools\android-user'
$env:JAVA_HOME='D:\Documents\GitHub\step_wise\.tools\java\jdk-17.0.20.1+1'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

cd android
.\gradlew.bat ':app:assembleDebug' ':app:testDebugUnitTest' '--offline'
```

Se voce estiver em outra maquina, ajuste os caminhos ou use o JDK 17 instalado no sistema.

### Instalar no celular

Com o celular conectado e autorizado no ADB:

```powershell
cd D:\Documents\GitHub\step_wise

$env:GRADLE_USER_HOME='D:\Documents\GitHub\step_wise\.tools\gradle-cache'
$env:ANDROID_USER_HOME='D:\Documents\GitHub\step_wise\.tools\android-user'
$env:JAVA_HOME='D:\Documents\GitHub\step_wise\.tools\java\jdk-17.0.20.1+1'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

cd android
.\gradlew.bat ':app:installDebug' '--offline'
cd ..
.\.tools\android\platform-tools\adb.exe shell monkey -p com.stepwise 1
```

Se usar ADB global:

```powershell
adb devices -l
adb shell monkey -p com.stepwise 1
```

Observacao: neste notebook, o Kotlin daemon pode emitir `AccessDeniedException` em `C:\Users\user\AppData\Local\kotlin\daemon`. O Gradle tem usado fallback e o build continua. O que importa e o final `BUILD SUCCESSFUL`.

## O que testar no app

Na tela principal:

1. Autorize as permissoes solicitadas.
2. Em **Demo controlada do procedimento**, teste:
   - `Simular acerto confiavel`;
   - `Simular erro conhecido`;
   - `Simular inconclusivo`;
   - `Verificar com imagem local`;
   - `Selecionar foto e verificar`;
   - `Tirar foto e verificar`;
   - `Ajuda`;
   - `Repetir instrucao`.
3. Em **Comandos de voz simulados**, teste:
   - `Voz: verificar`;
   - `Voz: ajuda`;
   - `Voz: repetir`;
   - `Voz: desconhecido`.
4. Em **Teste de microfone PCM**, toque em `Testar microfone por 3 segundos` e confirme que RMS/bytes nao ficam zerados.
5. Em **Diagnostico visual local**, toque em `Rodar diagnostico visual local` e confirme que aparecem resultados top/conf.

## Arquitetura atual do app

Caminho de decisao:

```text
UI / voz / fonte visual local
  -> StepwiseImageClassifier
  -> ProceduralEngine
  -> DemoSessionController
  -> UI + TtsManager
```

Classes importantes:

```text
android/app/src/main/java/com/stepwise/ui/MainActivity.kt
android/app/src/main/java/com/stepwise/session/DemoSessionController.kt
android/app/src/main/java/com/stepwise/engine/ProceduralEngine.kt
android/app/src/main/java/com/stepwise/audio/TtsManager.kt
android/app/src/main/java/com/stepwise/audio/VoiceInputManager.kt
android/app/src/main/java/com/stepwise/audio/MicrophoneCaptureManager.kt
android/app/src/main/java/com/stepwise/vision/StepwiseImageClassifier.kt
```

Assets importantes:

```text
android/app/src/main/assets/models/stepwise_real_classifier_v1.tflite
android/app/src/main/assets/models/stepwise_real_classifier_v1_labels.json
android/app/src/main/assets/models/stepwise_real_classifier_v1_manifest.json
android/app/src/main/assets/procedures/montagem_engrenagens.json
android/app/src/main/assets/diagnostic_samples/
```

## Dataset

O dataset real esta em:

```text
dataset stepwise/
```

Resumo atual:

- 581 imagens.
- Classes de pecas isoladas, estados de montagem e erros.
- Splits derivados em `ml/data/stepwise_real/`:
  - train: 411;
  - val: 85;
  - test: 85.

Regenerar splits a partir da raiz:

```powershell
python scripts/prepare_stepwise_dataset.py --dataset-root "dataset stepwise" --output-dir ml/data/stepwise_real
```

Detalhes completos em `docs/dataset-real-stepwise.md`.

## Treino do modelo visual

Use WSL/Ubuntu com Python 3.10. Exemplo:

```bash
cd /mnt/d/Documents/GitHub/step_wise
python3.10 -m venv ~/.venvs/stepwise-train
source ~/.venvs/stepwise-train/bin/activate
pip install -r ml/requirements-train.lock.txt
```

Treinar o classificador real:

```bash
python ml/train.py \
  --dataset-root "dataset stepwise" \
  --index-dir ml/data/stepwise_real \
  --output-dir ml/models/stepwise_real_classifier_v1 \
  --epochs 3
```

Avaliar TFLite:

```bash
python ml/evaluate.py \
  --dataset-root "dataset stepwise" \
  --index-dir ml/data/stepwise_real \
  --split test \
  --model ml/models/stepwise_real_classifier_v1/stepwise_real_classifier_v1.tflite \
  --output-dir ml/models/stepwise_real_classifier_v1/eval
```

Copiar modelo novo para o app, se substituir o atual:

```powershell
Copy-Item ml\models\stepwise_real_classifier_v1\stepwise_real_classifier_v1.tflite android\app\src\main\assets\models\stepwise_real_classifier_v1.tflite -Force
Copy-Item ml\models\stepwise_real_classifier_v1\labels.json android\app\src\main\assets\models\stepwise_real_classifier_v1_labels.json -Force
Copy-Item ml\models\stepwise_real_classifier_v1\manifest.json android\app\src\main\assets\models\stepwise_real_classifier_v1_manifest.json -Force
```

Depois recompile e rode os testes Android.

## Testes uteis

Unitarios locais:

```powershell
cd android
.\gradlew.bat ':app:testDebugUnitTest' '--offline'
```

Build debug:

```powershell
cd android
.\gradlew.bat ':app:assembleDebug' '--offline'
```

Instrumentado no POCO, quando conectado:

```powershell
cd android
.\gradlew.bat ':app:connectedDebugAndroidTest' '--offline'
```

## Integracao com oculos

A integracao real com Ray-Ban Meta ainda depende do hardware no evento.

Preparacao ja feita:

- Meta AI instalado e Developer Mode ativado no celular.
- CameraAccess oficial do DAT foi compilado/instalado e testado com Mock Device Kit.
- Roteiro offline de registro real esta em `docs/meta-ai-registro-real.md`.

No evento, a fonte visual local deve ser trocada por frames/capturas vindos do DAT, mantendo o mesmo contrato:

```text
frame dos oculos -> pre-processamento -> StepwiseImageClassifier/Detector -> ProceduralEngine -> UI/TTS
```

## Limites conhecidos

- O modelo visual atual tem qualidade baixa e deve ser tratado como prova de pipeline.
- O motor ja e conservador: baixa confianca, classe inesperada ou imagem ruim devem virar `INCONCLUSIVE`.
- Reconhecimento de voz real offline ainda nao esta conectado a Vosk/Sherpa; hoje existem parser, botoes simulados e captura PCM validada.
- A camera do celular e uma simulacao local; a perspectiva dos oculos precisa ser validada no evento.
- Fading/persistencia completa ainda precisa de ciclo adicional com Room/controlador real.

## Proximo roteiro recomendado

1. Ensaiar a demo completa no POCO com o kit fisico.
2. Registrar um roteiro de apresentacao: ordem de cliques, falas e explicacao dos limites.
3. Melhorar o modelo visual ou preparar contingencia deterministica marcada/HSV/ArUco.
4. Conectar reconhecimento offline real aos comandos `verificar`, `ajuda`, `repetir`.
5. No evento, trocar a fonte local de imagem pela fonte DAT dos oculos.

## Referencias internas

- Plano final: `docs/PLANO_ACAO_FINAL_STEPWISE.md`
- Ambiente validado: `docs/ambiente-validado.md`
- Dataset/modelo: `docs/dataset-real-stepwise.md`
- Treino B17-B19: `docs/b17-b19-treino-exportacao.md`
- CameraAccess/DAT: `docs/cameraaccess-validacao.md`
- Registro real Meta AI/DAT: `docs/meta-ai-registro-real.md`
