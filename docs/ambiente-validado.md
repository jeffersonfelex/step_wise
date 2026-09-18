# StepWise â€” inventÃ¡rio e validaÃ§Ã£o do ambiente

Este arquivo acompanha os aparelhos, ferramentas e testes durante a preparaÃ§Ã£o para a final. O nome `ambiente-validado.md` segue o caminho previsto no roadmap; **nem tudo registrado aqui estÃ¡ validado**. Cada informaÃ§Ã£o possui origem e estado explÃ­citos.

**ReferÃªncia:** [plano de aÃ§Ã£o para a final](PLANO_ACAO_FINAL_STEPWISE.md).

## 1. Celular-alvo

| Campo | InformaÃ§Ã£o registrada | Origem / estado |
|---|---|---|
| Modelo | POCO X8 Pro Max | Informado pelo usuÃ¡rio. |
| Android | 16 | Confirmado via ADB em 16/09/2026. |
| Build Android | `BP2A.250605.031.A3` | Confirmado via ADB em 16/09/2026. |
| HyperOS completo | `3.0.303.0.WPLMIXM.C11` | TranscriÃ§Ã£o exata da resposta do usuÃ¡rio. |
| RAM fÃ­sica | 12 GB | Informado pelo usuÃ¡rio. |
| Armazenamento livre | 146,3 GB | Informado pelo usuÃ¡rio; valor pontual, sujeito a mudanÃ§a. |
| Aparelho em mÃ£os | Sim | Em 16/09/2026, usuÃ¡rio confirmou POCO conectado por USB, com transferÃªncia de arquivos ativada. |
| Cabo USB de dados | Sim | ConexÃ£o ADB validada e usada em instalaÃ§Ãµes/testes. |
| Meta AI na Play Store | DisponÃ­vel | Informado pelo usuÃ¡rio. |
| Meta AI instalado | Sim | Informado pelo usuÃ¡rio. |
| VersÃ£o do aplicativo Meta AI | `289.0.0.25.162`, cÃ³digo `968902270` | Confirmada por ADB no pacote `com.facebook.stella` em 16/09/2026. |
| Developer Mode no Meta AI | Ativado | Confirmado pelo usuÃ¡rio em 16/09/2026, em InformaÃ§Ãµes do app apÃ³s cinco toques na versÃ£o. Conferir novamente com os Ã³culos vinculados. |
| Conta/login no Meta AI | ConcluÃ­do | Informado pelo usuÃ¡rio em 16/09/2026; registro DAT e pareamento real ainda nÃ£o validados. |
| API Android reportada pelo dispositivo | 36 | Confirmada por ADB em 16/09/2026, junto de Android 16 e build `BP2A.250605.031.A3`. |
| ABI reportada pelo dispositivo | `arm64-v8a` | Confirmada por ADB; modelo tÃ©cnico `2602BPC18G`. |
| DepuraÃ§Ã£o USB / autorizaÃ§Ã£o do notebook | Validadas | Um dispositivo USB no estado `device`; propriedades lidas com sucesso. |
| Ã“culos disponÃ­veis antes do evento | NÃ£o disponÃ­veis agora | Confirmado pelo usuÃ¡rio em 16/09/2026; testes fÃ­sicos dependem de acesso posterior ao hardware. |
| Kit fÃ­sico de engrenagens | NÃ£o identificado | NecessÃ¡rio para unificar procedimento, coleta e detector. |

### Leitura para o planejamento

- O Android 16 informado estÃ¡ acima da baseline Android 12/API 31 adotada no plano. Isso permite seguir com essa baseline sem propor a troca do aparelho apenas por versÃ£o do sistema.
- RAM e espaÃ§o livre informados nÃ£o indicam necessidade imediata de reduzir o escopo de armazenamento do protÃ³tipo. Consumo de memÃ³ria, tamanho dos modelos e desempenho ainda serÃ£o medidos.
- A disponibilidade do cabo e a instalaÃ§Ã£o do Meta AI permitem avanÃ§ar para a preparaÃ§Ã£o da conexÃ£o e do aplicativo de referÃªncia.
- Esses dados **nÃ£o comprovam** compatibilidade final com DAT, reconhecimento offline, microfone dos Ã³culos, saÃ­da de Ã¡udio, cÃ¢mera simultÃ¢nea com HFP ou latÃªncia do detector.

## 2. Notebook e ferramentas

O ambiente Ã© Windows/PowerShell, em `D:\Documents\GitHub\step_wise`. Em 16/09/2026, o usuÃ¡rio confirmou que este notebook serÃ¡ levado ao evento. InventÃ¡rio abaixo obtido por comandos locais; ausÃªncia de ferramenta significa que ela nÃ£o foi localizada no PATH, registros e diretÃ³rios de instalaÃ§Ã£o inspecionados.

| Item | Estado atual |
|---|---|
| Notebook do evento: modelo, RAM e espaÃ§o livre | Lenovo `80YF`; aproximadamente 8 GB de RAM (7,9 GiB reportados). Livres: C: 65,8 GiB; D: 464,1 GiB, valores pontuais. |
| Sistema e versÃ£o do notebook do evento | Windows 11 Pro, 64 bits, `10.0.22621`. |
| Android Studio / versÃ£o | Instalado pelo usuÃ¡rio e localizado em `C:\Program Files\Android\Android Studio`. VersÃ£o identificada pelo diretÃ³rio de configuraÃ§Ã£o: `2026.1.2`; build `261.25134.95.2612.15822958` em `product-info.json`. |
| JDK usado pelo Gradle | Temurin `17.0.20.1+1` em `.tools/java/jdk-17.0.20.1+1`; usado com sucesso para compilar o APK e executar teste. IDE configurada localmente; sincronizaÃ§Ã£o pela interface ainda nÃ£o observada. Studio mantÃ©m JBR 21.0.10. |
| Android SDK / platform-tools / ADB | SDK em `C:\Users\user\AppData\Local\Android\Sdk`, plataformas android-36 e android-36.1, Build-Tools 36.0.0 e Platform-Tools 37.0.1. SDK 36 utilizado no build aprovado. ADB portÃ¡til em `.tools/android/platform-tools`. |
| Git | `2.55.0.windows.5`, validado por `C:\Program Files\Git\cmd\git.exe --version`; fora do PATH do terminal atual. |
| Editores e auxiliares | VS Code `1.137.0` e GitHub Desktop `3.6.5`, versÃµes dos registros de instalaÃ§Ã£o. |
| VariÃ¡veis do ambiente Android | `JAVA_HOME`, `ANDROID_HOME` e `ANDROID_SDK_ROOT` ausentes nos escopos processo, usuÃ¡rio e mÃ¡quina. PATH global nÃ£o alterado. |
| Python da sessÃ£o anterior | `3.14.7`; nÃ£o confundir com o Python 3.10 isolado proposto para treinamento. |
| Python 3.10 e ferramentas Windows | Python 3.10.11 x64 em `.tools/python310/python.exe`, instalado via `py install --target=.tools/python310 3.10-64`; `.venv-tools` isolado. Python global permanece 3.14.7. |
| Bibliotecas de ferramentas | NumPy 1.26.4, Pillow 12.3.0, opencv-contrib-python 4.11.0.86, pytest 9.1.1. `pip check`, imports, PNG OpenCVâ†’Pillow e disponibilidade ArUco aprovados em 16/09/2026. Lock: `ml/requirements-tools.lock.txt`. |
| WSL / Linux de treino | Ubuntu WSL2 instalado e validado em 16/09/2026. Python 3.10.21 instalado via `uv`; ambiente de treino em `~/.venvs/stepwise-train`. `pip check` e import de Model Maker passaram. |
| Model Maker / treino smoke | `mediapipe-model-maker==0.2.1.4`, TensorFlow 2.15.1, MediaPipe 0.10.9, Protobuf 3.20.3. Lock em `ml/requirements-train.lock.txt`. Smoke COCO sintetico exportou `ml/models/b19_smoke/stepwise_b19_smoke.tflite`, SHA256 `F6A682702E2CD535C31AC8437CB009C73B97101DB445DE0E72FDC90793F86CE9`, 11.240.068 bytes. |
| Matriz Gradle/Kotlin/DAT/Room/IA | Gradle 8.14.1, AGP 8.11.1, Kotlin 2.2.21, KSP 2.2.21-2.0.5, Room 2.8.4, DAT 0.9.0 e TensorFlow Lite 2.15.0 compilados em debug. IA smoke carregada e executada no POCO. |

## 3. Estado das tarefas relacionadas

| Tarefa | Estado | O que falta para concluir |
|---|---|---|
| A01 â€” inventÃ¡rio do aparelho e notebook | **ConcluÃ­da** | Dados exigidos registrados; API/ABI confirmadas via ADB. |
| B01 â€” localizar ferramentas | **ConcluÃ­da** | Git e ADB executados pelos caminhos explÃ­citos; instalaÃ§Ãµes faltantes identificadas para B03/B04. |
| A02 â€” disponibilidade dos Ã³culos e kit | **Pendente** | Identificar kit e acesso prÃ©vio ao hardware. |
| B05 â€” depuraÃ§Ã£o USB | **ConcluÃ­da** | POCO autorizado, estado `device` e consultas de propriedades bem-sucedidas em 16/09/2026. |
| B03 â€” Android Studio e JDK | **Parcial** | Gradle Wrapper 8.14.1 executou `--version` e `help` com JDK 17; configuraÃ§Ã£o do aplicativo carregada com sucesso. SeleÃ§Ã£o da IDE preparada; sincronizaÃ§Ã£o pela interface ainda nÃ£o observada. |
| B04 â€” pacotes SDK | **ConcluÃ­da** | APK debug compilado com SDK 36 e Build-Tools 36.0.0. |
| B09/B10 â€” Wrapper, catÃ¡logo e matriz | **ConcluÃ­das** | APK e teste unitÃ¡rio aprovados; arquivos preparados para versionamento. |
| B08 â€” Meta AI e preparaÃ§Ã£o de registro | **ConcluÃ­da: preparaÃ§Ã£o** | VersÃ£o via ADB; login e Developer Mode confirmados pelo usuÃ¡rio; roteiro em meta-ai-registro-real.md. Registro/pareamento reais dependem dos Ã³culos. |
| B26 â€” ambiente efetivamente validado | **Parcial: registro criado** | Preencher versÃµes e evidÃªncias depois dos testes; existÃªncia deste arquivo nÃ£o conclui B26. |

## 4. PrÃ³ximo passo

**PrÃ³xima aÃ§Ã£o de inventÃ¡rio:** confirmar disponibilidade do kit e Ã³culos em A02. InventÃ¡rio A01 concluÃ­do.

**B06â€“B08 e B13â€“B20 concluÃ­das.** PrÃ³xima aÃ§Ã£o: fechar B21/B22 do ambiente ou avanÃ§ar contratos/motor C/D, conforme prioridade. B21 tem locks Windows e treino; recriaÃ§Ã£o B22 pendente. Venvs ignorados pelo Git. Ver [execuÃ§Ã£o em blocos](execucao-em-blocos.md) e [b17-b19-treino-exportacao.md](b17-b19-treino-exportacao.md). Registro real depende dos Ã³culos; detalhes em [meta-ai-registro-real.md](meta-ai-registro-real.md).

Comandos para repetir a validaÃ§Ã£o a partir da raiz do repositÃ³rio, sem alterar o PATH global:

```powershell
& '.\.tools\android\platform-tools\adb.exe' devices -l
& '.\.tools\android\platform-tools\adb.exe' -d shell getprop ro.build.version.release
& '.\.tools\android\platform-tools\adb.exe' -d shell getprop ro.build.version.sdk
& '.\.tools\android\platform-tools\adb.exe' -d shell getprop ro.build.display.id
& '.\.tools\android\platform-tools\adb.exe' -d shell getprop ro.product.cpu.abilist
```

Se houver mais de um dispositivo USB conectado, selecionar explicitamente o aparelho-alvo com `-s <serial>` no lugar de `-d`. O identificador nÃ£o foi publicado. As consultas foram executadas em 16/09/2026 e retornaram Android `16`, API `36`, build `BP2A.250605.031.A3` e ABI `arm64-v8a`.

Platform-tools baixado da [distribuiÃ§Ã£o oficial do Google](https://developer.android.com/tools/releases/platform-tools). `.tools/` estÃ¡ ignorada pelo Git e nÃ£o acompanha o clone. O ADB criou sua pasta padrÃ£o de autorizaÃ§Ã£o no perfil do Windows e iniciou o servidor local na porta 5037. O PATH global nÃ£o foi alterado. Git pode ser executado por `C:\Program Files\Git\cmd\git.exe`.

## 5. Registro de testes

### JDK 17 local

- DistribuiÃ§Ã£o: Eclipse Temurin Windows x64, `17.0.20.1+1`, obtida pela [API oficial Adoptium](https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse).
- SHA-256 conferido contra metadados oficiais: `E53A79C3C3D86865BD7E787903884331068E71321714FFD44F145785AFFC7CB0`.
- `android/.gradle/config.properties` define `java.home`; `android/.idea/gradle.xml` seleciona `GRADLE_LOCAL_JAVA_HOME`, conforme a [documentaÃ§Ã£o Android](https://developer.android.com/build/jdks). Ambos sÃ£o locais, ignorados pelo Git.
- Abrir a pasta **android** como projeto no Studio. A seleÃ§Ã£o configurada deve aparecer em Settings â†’ Build, Execution, Deployment â†’ Build Tools â†’ Gradle. Wrapper 8.14.1 adicionado e `--version` validado; sincronizaÃ§Ã£o da IDE ainda nÃ£o validada.
- Para abrir um terminal com esse JDK, executar `scripts\terminal-java17.cmd` a partir da raiz. `scripts\terminal-java17.cmd --check` validou `java` e `javac` na versÃ£o 17.0.20.1. JAVA_HOME/PATH mudam somente no terminal criado, sem alteraÃ§Ã£o global. Foi usado CMD porque a polÃ­tica atual do PowerShell bloqueia scripts; essa polÃ­tica foi preservada.
- A instalaÃ§Ã£o estÃ¡ em `.tools/`, nÃ£o acompanha clones. Em outro notebook, instalar o JDK e ajustar os caminhos locais. NÃ£o confundir essa configuraÃ§Ã£o com build Android aprovado.

A conexÃ£o ADB e as propriedades do POCO foram validadas em 16/09/2026. Testes do aplicativo continuam pendentes.

| Teste | Resultado | EvidÃªncia |
|---|---|---|
| B13 â€” Room no POCO | Aprovado em 16/09/2026 | 2 testes, 0 falhas/erros/ignorados. Insert/read, reabertura e isolamento de procedimentos; relatÃ³rio em android/app/build/reports/androidTests/connected/debug/index.html. |
| CompilaÃ§Ã£o APK debug + teste unitÃ¡rio | Aprovados em 16/09/2026 | BUILD SUCCESSFUL; 1 teste, 0 falhas/erros. APK em `android/app/build/outputs/apk/debug/app-debug.apk`. |
| Gradle/JDK e configuraÃ§Ã£o Android | Aprovado em 16/09/2026 | Wrapper 8.14.1 com Temurin 17.0.20.1; tarefa `help` com BUILD SUCCESSFUL. NÃ£o equivale a APK compilado. |
| ConexÃ£o USB/ADB | Aprovada em 16/09/2026 | ADB 37.0.1; um dispositivo `device`; consultas de Android/API/build/ABI concluÃ­das. |
| InstalaÃ§Ã£o e abertura do APK StepWise | Aprovadas em 16/09/2026 | ApÃ³s usuÃ¡rio habilitar instalaÃ§Ã£o USB: install Success; abertura e reabertura Status ok; versÃ£o 0.1.0-mvp/build 1. |
| PermissÃµes Android e tela | Estados conferidos no POCO | UI inicialmente nÃ£o autorizada; depois trÃªs permissÃµes granted=true e UI correspondente, preservadas apÃ³s reabertura. Recusa/bloqueio permanente nÃ£o exercitados. |
| Build offline com tela mÃ­nima | Aprovado em 16/09/2026 | assembleDebug e testDebugUnitTest --offline: sucesso, 1 teste aprovado. |
| CameraAccess / DAT com MDK | Aprovado no escopo B06/B07 | Exemplo oficial 0.9.0 instalado; 1 dispositivo mock pareado, sessÃ£o started â†’ stopped; sem stream/mÃ­dia. MDK desabilitado ao encerrar. |
| Modelo visual no celular | Aprovado no escopo B20 smoke | 1 teste instrumentado no POCO Android 16: asset `.tflite` carregado via TensorFlow Lite Interpreter, entrada sintÃ©tica 256x256 executada, saÃ­das boxes/classes/scores/count presentes. RelatÃ³rio em `android/app/build/reports/androidTests/connected/debug/index.html`. |
| Voz e TTS offline | Parcial aprovado no celular | Android TTS integrado a demo controlada; `assembleDebug`, `testDebugUnitTest`, `installDebug` e abertura no POCO aprovados em 17/09/2026. Usuario confirmou audio em todos os botoes da demo. Modo aviao e rota Bluetooth/Ray-Ban pendentes. |
| CÃ¢mera, microfone e Ã¡udio dos Ray-Ban Meta | NÃ£o executado | â€” |
| LatÃªncia, bateria e estabilidade | NÃ£o executado | â€” |

Ao executar um teste, registrar data, versÃ£o do app/modelo, aparelho, condiÃ§Ãµes, resultado e caminho da evidÃªncia. Resultados de mock, cÃ¢mera do celular e Ã³culos reais devem permanecer identificados.

## 6. Como manter a documentaÃ§Ã£o durante a execuÃ§Ã£o

- **Roadmap:** tarefas, dependÃªncias, critÃ©rios de conclusÃ£o e indicaÃ§Ã£o do prÃ³ximo passo.
- **Este arquivo:** inventÃ¡rio, versÃµes realmente instaladas e evidÃªncias de testes do ambiente.
- **Registro de decisÃµes, quando houver escolhas implementadas:** motivos, alternativas e resultado dos experimentos; caminho previsto `docs/decisoes-final.md`.
- **RelatÃ³rios e logs:** evidÃªncias detalhadas geradas nos testes, referenciadas pelos documentos; nÃ£o substituir resultados por descriÃ§Ãµes de intenÃ§Ã£o.
- **Documentos submetidos e e-mails:** manter como histÃ³rico. Atualizar a documentaÃ§Ã£o de implementaÃ§Ã£o e os materiais futuros, preservando os arquivos originais.

Registrar informaÃ§Ãµes quando forem confirmadas e marcar tarefas completas somente apÃ³s o critÃ©rio de conclusÃ£o. NÃ£o Ã© necessÃ¡rio criar um documento novo para cada conversa.

## 7. HistÃ³rico

| AtualizaÃ§Ã£o | AlteraÃ§Ã£o |
|---|---|
| 18/09/2026 - camera local validada | Usuario confirmou que a demo `Tirar foto e verificar` funcionou corretamente no POCO. Validacao cobre camera do celular -> modelo -> motor -> UI/TTS. |
| 17/09/2026 - camera local implementada | Botao `Tirar foto e verificar` adicionado. Usa `TakePicturePreview`, classifica o bitmap capturado e envia top-1 ao motor conservador com UI/TTS. Build/testes/instalacao/open no POCO aprovados. |
| 17/09/2026 - foto local validada | Usuario selecionou foto no POCO; demo controlada rodou e teve a saida esperada. Validacao cobre fonte local escolhida pelo usuario -> modelo -> motor -> UI/TTS. |
| 17/09/2026 - seletor de imagem local | Botao `Selecionar foto e verificar` adicionado. Usa seletor nativo `GetContent`, decodifica a imagem escolhida, roda `StepwiseImageClassifier` e envia top-1 ao motor conservador com UI/TTS. Build/testes/instalacao/open no POCO aprovados. |
| 17/09/2026 - ciclo visual local validado | Usuario clicou em `Verificar com imagem local` no POCO; app retornou texto e audio corretamente. Validacao fecha o ciclo local imagem -> modelo -> motor -> UI/TTS. |
| 17/09/2026 - visual ligado ao motor | Botao `Verificar com imagem local` adicionado. Ele escolhe a imagem asset da etapa atual, roda `StepwiseImageClassifier`, converte top-1 em evidencia e deixa `ProceduralEngine` decidir com limiar conservador. Build/testes/instalacao/open no POCO aprovados. |
| 17/09/2026 - diagnostico visual validado | Usuario executou o diagnostico visual local no POCO e confirmou retorno de resultados. Validacao cobre inferencia local em assets; qualidade do modelo continua registrada como insuficiente para avanco automatico. |
| 17/09/2026 - diagnostico visual local | `StepwiseImageClassifier` criado para rodar `stepwise_real_classifier_v1.tflite` em imagens reais empacotadas nos assets. UI recebeu botao de diagnostico visual local. Build/testes/instalacao/open no POCO aprovados. |
| 17/09/2026 - microfone validado | Usuario executou o teste de microfone no POCO; captura concluiu e valores nao ficaram zerados. Validacao cobre microfone do celular; origem Bluetooth/Ray-Ban segue pendente. |
| 17/09/2026 - captura PCM microfone | `MicrophoneCaptureManager` implementado com `AudioRecord` em PCM 16 kHz mono por janelas de 3 segundos. UI mostra RMS e bytes capturados. Build/testes/instalacao/open no POCO aprovados. |
| 17/09/2026 - comandos de voz simulados | `VoiceInputManager` passou a mapear transcricoes para VERIFY/HELP/REPEAT/UNKNOWN. Tela recebeu botoes de voz simulada; build/testes unitarios/instalacao/open no POCO aprovados. Reconhecimento offline real por modelo ASR ainda pendente. |
| 17/09/2026 - TTS audivel | Usuario confirmou no POCO que todos os botoes da demo controlada reproduziram audio. Validacao cobre saida pelo celular; modo aviao, Bluetooth e Ray-Ban permanecem pendentes. |
| 17/09/2026 - F01/F02/F15 | TTS Android integrado ao modo demo/controlado. Botoes da demo disparam fala para feedback e instrucao; `TtsManager` usa `pt-BR`, callbacks de inicio/fim/falha e fallback de idioma. Build/teste/instalacao/open no POCO aprovados; audicao fisica ainda precisa ser confirmada no aparelho. |
| 16/09/2026 â€” B08 | UsuÃ¡rio confirmou login e Developer Mode ativado no Meta AI, acessado por InformaÃ§Ãµes do app e cinco toques na versÃ£o. Roteiro offline concluÃ­do; Ã³culos indisponÃ­veis, registro/pareamento reais pendentes. |
| 16/09/2026 â€” B18/B19 | Ambiente Linux de treino validado com Model Maker; smoke COCO sintetico treinou/exportou `.tflite`. Artefato local ignorado pelo Git; proximo passo B20 no Android. |
| 16/09/2026 â€” B20 | `stepwise_b19_smoke.tflite` copiado para assets Android e validado no POCO com `TfliteSmokeInferenceTest`. APK debug SHA256 `C7DC5D469EFE05990FC895B9BED72E3D050CBE898CAFB87992CDCA52C8FCF802`; APK de teste SHA256 `2B33474DC3154FA275CE7A316DAEE4141FA14A31F9163E0D90EC6F51B836C332`. |
| 16/09/2026 â€” B17 | WSL, Plataforma de MÃ¡quina Virtual e Ubuntu instalados pelo instalador oficial. ApÃ³s reboot e primeira inicializaÃ§Ã£o, Ubuntu WSL2 ficou operacional. |
| 16/09/2026 â€” B06/B07 | ReferÃªncia oficial fixada no commit 974e05c; CameraAccess compilado, instalado e sessÃ£o MDK testada. Meta AI 289.0.0.25.162 identificado. Build-Tools 35.0.0 adicionado pelo exemplo oficial. |
| 16/09/2026 â€” B13 | 2 testes instrumentados Room aprovados no POCO. Executor removeu APKs; StepWise reinstalado e aberto com sucesso. PermissÃµes zeradas pela reinstalaÃ§Ã£o. |
| 16/09/2026 â€” B11/B12/B14 | Tela mÃ­nima instalada no POCO; duas aberturas bem-sucedidas, permissÃµes autorizadas refletidas na UI. Build offline e teste unitÃ¡rio aprovados. |
| 16/09/2026 â€” JDK 17 | Temurin instalado com checksum verificado; java/javac executados; configuraÃ§Ã£o local da IDE e terminal preparada. API 36 tambÃ©m confirmada no disco. Gradle/build continuam pendentes. |
| 16/09/2026 â€” apÃ³s instalaÃ§Ã£o do Studio pelo usuÃ¡rio | InspeÃ§Ã£o confirmou Studio, JBR 21.0.10, SDK com android-36.1, Build-Tools 36.0.0 e Platform-Tools 37.0.1. B03/B04 parciais; sem build ou alteraÃ§Ã£o das versÃµes do projeto. |
| 16/09/2026 â€” validaÃ§Ã£o USB | ADB portÃ¡til instalado; POCO autorizado e propriedades lidas. A01, B01 e B05 concluÃ­das. SDK completo e APK ainda pendentes. |
| 16/09/2026 â€” inspeÃ§Ã£o B01 | Notebook do evento inventariado; Git e JRE executados; Python 3.14.7 identificado. Android Studio, JDK e SDK/ADB anteriores nÃ£o localizados. PresenÃ§a fÃ­sica do POCO confirmada pelo usuÃ¡rio. |
| InventÃ¡rio inicial informado pelo usuÃ¡rio | Registrados POCO X8 Pro Max, Android/build, HyperOS, RAM, espaÃ§o livre, cabo e Meta AI instalado. Disponibilidade fÃ­sica preservada como pendente devido Ã  resposta ambÃ­gua. Nenhum teste fÃ­sico declarado como concluÃ­do. |
