# StepWise — inventário e validação do ambiente

Este arquivo acompanha os aparelhos, ferramentas e testes durante a preparação para a final. O nome `ambiente-validado.md` segue o caminho previsto no roadmap; **nem tudo registrado aqui está validado**. Cada informação possui origem e estado explícitos.

**Referência:** [plano de ação para a final](PLANO_ACAO_FINAL_STEPWISE.md).

## 1. Celular-alvo

| Campo | Informação registrada | Origem / estado |
|---|---|---|
| Modelo | POCO X8 Pro Max | Informado pelo usuário. |
| Android | 16 | Confirmado via ADB em 16/09/2026. |
| Build Android | `BP2A.250605.031.A3` | Confirmado via ADB em 16/09/2026. |
| HyperOS completo | `3.0.303.0.WPLMIXM.C11` | Transcrição exata da resposta do usuário. |
| RAM física | 12 GB | Informado pelo usuário. |
| Armazenamento livre | 146,3 GB | Informado pelo usuário; valor pontual, sujeito a mudança. |
| Aparelho em mãos | Sim | Em 16/09/2026, usuário confirmou POCO conectado por USB, com transferência de arquivos ativada. |
| Cabo USB de dados | Sim | Conexão ADB validada e usada em instalações/testes. |
| Meta AI na Play Store | Disponível | Informado pelo usuário. |
| Meta AI instalado | Sim | Informado pelo usuário. |
| Versão do aplicativo Meta AI | `289.0.0.25.162`, código `968902270` | Confirmada por ADB no pacote `com.facebook.stella` em 16/09/2026. |
| Developer Mode no Meta AI | Ativado | Confirmado pelo usuário em 16/09/2026, em Informações do app após cinco toques na versão. Conferir novamente com os óculos vinculados. |
| Conta/login no Meta AI | Concluído | Informado pelo usuário em 16/09/2026; registro DAT e pareamento real ainda não validados. |
| API Android reportada pelo dispositivo | 36 | Confirmada por ADB em 16/09/2026, junto de Android 16 e build `BP2A.250605.031.A3`. |
| ABI reportada pelo dispositivo | `arm64-v8a` | Confirmada por ADB; modelo técnico `2602BPC18G`. |
| Depuração USB / autorização do notebook | Validadas | Um dispositivo USB no estado `device`; propriedades lidas com sucesso. |
| Óculos disponíveis antes do evento | Não disponíveis agora | Confirmado pelo usuário em 16/09/2026; testes físicos dependem de acesso posterior ao hardware. |
| Kit físico de engrenagens | Não identificado | Necessário para unificar procedimento, coleta e detector. |

### Leitura para o planejamento

- O Android 16 informado está acima da baseline Android 12/API 31 adotada no plano. Isso permite seguir com essa baseline sem propor a troca do aparelho apenas por versão do sistema.
- RAM e espaço livre informados não indicam necessidade imediata de reduzir o escopo de armazenamento do protótipo. Consumo de memória, tamanho dos modelos e desempenho ainda serão medidos.
- A disponibilidade do cabo e a instalação do Meta AI permitem avançar para a preparação da conexão e do aplicativo de referência.
- Esses dados **não comprovam** compatibilidade final com DAT, reconhecimento offline, microfone dos óculos, saída de áudio, câmera simultânea com HFP ou latência do detector.

## 2. Notebook e ferramentas

O ambiente é Windows/PowerShell, em `D:\Documents\GitHub\step_wise`. Em 16/09/2026, o usuário confirmou que este notebook será levado ao evento. Inventário abaixo obtido por comandos locais; ausência de ferramenta significa que ela não foi localizada no PATH, registros e diretórios de instalação inspecionados.

| Item | Estado atual |
|---|---|
| Notebook do evento: modelo, RAM e espaço livre | Lenovo `80YF`; aproximadamente 8 GB de RAM (7,9 GiB reportados). Livres: C: 65,8 GiB; D: 464,1 GiB, valores pontuais. |
| Sistema e versão do notebook do evento | Windows 11 Pro, 64 bits, `10.0.22621`. |
| Android Studio / versão | Instalado pelo usuário e localizado em `C:\Program Files\Android\Android Studio`. Versão identificada pelo diretório de configuração: `2026.1.2`; build `261.25134.95.2612.15822958` em `product-info.json`. |
| JDK usado pelo Gradle | Temurin `17.0.20.1+1` em `.tools/java/jdk-17.0.20.1+1`; usado com sucesso para compilar o APK e executar teste. IDE configurada localmente; sincronização pela interface ainda não observada. Studio mantém JBR 21.0.10. |
| Android SDK / platform-tools / ADB | SDK em `C:\Users\user\AppData\Local\Android\Sdk`, plataformas android-36 e android-36.1, Build-Tools 36.0.0 e Platform-Tools 37.0.1. SDK 36 utilizado no build aprovado. ADB portátil em `.tools/android/platform-tools`. |
| Git | `2.55.0.windows.5`, validado por `C:\Program Files\Git\cmd\git.exe --version`; fora do PATH do terminal atual. |
| Editores e auxiliares | VS Code `1.137.0` e GitHub Desktop `3.6.5`, versões dos registros de instalação. |
| Variáveis do ambiente Android | `JAVA_HOME`, `ANDROID_HOME` e `ANDROID_SDK_ROOT` ausentes nos escopos processo, usuário e máquina. PATH global não alterado. |
| Python da sessão anterior | `3.14.7`; não confundir com o Python 3.10 isolado proposto para treinamento. |
| Python 3.10 e ferramentas Windows | Python 3.10.11 x64 em `.tools/python310/python.exe`, instalado via `py install --target=.tools/python310 3.10-64`; `.venv-tools` isolado. Python global permanece 3.14.7. |
| Bibliotecas de ferramentas | NumPy 1.26.4, Pillow 12.3.0, opencv-contrib-python 4.11.0.86, pytest 9.1.1. `pip check`, imports, PNG OpenCVâ†’Pillow e disponibilidade ArUco aprovados em 16/09/2026. Lock: `ml/requirements-tools.lock.txt`. |
| WSL / Linux de treino | Ubuntu WSL2 instalado e validado em 16/09/2026. Python 3.10.21 instalado via `uv`; ambiente de treino em `~/.venvs/stepwise-train`. `pip check` e import de Model Maker passaram. |
| Model Maker / treino smoke | `mediapipe-model-maker==0.2.1.4`, TensorFlow 2.15.1, MediaPipe 0.10.9, Protobuf 3.20.3. Lock em `ml/requirements-train.lock.txt`. Smoke COCO sintetico exportou `ml/models/b19_smoke/stepwise_b19_smoke.tflite`, SHA256 `F6A682702E2CD535C31AC8437CB009C73B97101DB445DE0E72FDC90793F86CE9`, 11.240.068 bytes. |
| Matriz Gradle/Kotlin/DAT/Room/IA | Gradle 8.14.1, AGP 8.11.1, Kotlin 2.2.21, KSP 2.2.21-2.0.5, Room 2.8.4, DAT 0.9.0 e TensorFlow Lite 2.15.0 compilados em debug. IA smoke carregada e executada no POCO. |

## 3. Estado das tarefas relacionadas

| Tarefa | Estado | O que falta para concluir |
|---|---|---|
| A01 — inventário do aparelho e notebook | **Concluída** | Dados exigidos registrados; API/ABI confirmadas via ADB. |
| B01 — localizar ferramentas | **Concluída** | Git e ADB executados pelos caminhos explícitos; instalações faltantes identificadas para B03/B04. |
| A02 — disponibilidade dos óculos e kit | **Pendente** | Identificar kit e acesso prévio ao hardware. |
| B05 — depuração USB | **Concluída** | POCO autorizado, estado `device` e consultas de propriedades bem-sucedidas em 16/09/2026. |
| B03 — Android Studio e JDK | **Parcial** | Gradle Wrapper 8.14.1 executou `--version` e `help` com JDK 17; configuração do aplicativo carregada com sucesso. Seleção da IDE preparada; sincronização pela interface ainda não observada. |
| B04 — pacotes SDK | **Concluída** | APK debug compilado com SDK 36 e Build-Tools 36.0.0. |
| B09/B10 — Wrapper, catálogo e matriz | **Concluídas** | APK e teste unitário aprovados; arquivos preparados para versionamento. |
| B08 — Meta AI e preparação de registro | **Concluída: preparação** | Versão via ADB; login e Developer Mode confirmados pelo usuário; roteiro em meta-ai-registro-real.md. Registro/pareamento reais dependem dos óculos. |
| B26 — ambiente efetivamente validado | **Parcial: registro criado** | Preencher versões e evidências depois dos testes; existência deste arquivo não conclui B26. |

## 4. Próximo passo

**Próxima ação de inventário:** confirmar disponibilidade do kit e óculos em A02. Inventário A01 concluído.

**B06–B08 e B13–B20 concluídas.** Próxima ação: fechar B21/B22 do ambiente ou avançar contratos/motor C/D, conforme prioridade. B21 tem locks Windows e treino; recriação B22 pendente. Venvs ignorados pelo Git. Ver [execução em blocos](execucao-em-blocos.md) e [b17-b19-treino-exportacao.md](b17-b19-treino-exportacao.md). Registro real depende dos óculos; detalhes em [meta-ai-registro-real.md](meta-ai-registro-real.md).

Comandos para repetir a validação a partir da raiz do repositório, sem alterar o PATH global:

```powershell
& '.\.tools\android\platform-tools\adb.exe' devices -l
& '.\.tools\android\platform-tools\adb.exe' -d shell getprop ro.build.version.release
& '.\.tools\android\platform-tools\adb.exe' -d shell getprop ro.build.version.sdk
& '.\.tools\android\platform-tools\adb.exe' -d shell getprop ro.build.display.id
& '.\.tools\android\platform-tools\adb.exe' -d shell getprop ro.product.cpu.abilist
```

Se houver mais de um dispositivo USB conectado, selecionar explicitamente o aparelho-alvo com `-s <serial>` no lugar de `-d`. O identificador não foi publicado. As consultas foram executadas em 16/09/2026 e retornaram Android `16`, API `36`, build `BP2A.250605.031.A3` e ABI `arm64-v8a`.

Platform-tools baixado da [distribuição oficial do Google](https://developer.android.com/tools/releases/platform-tools). `.tools/` está ignorada pelo Git e não acompanha o clone. O ADB criou sua pasta padrão de autorização no perfil do Windows e iniciou o servidor local na porta 5037. O PATH global não foi alterado. Git pode ser executado por `C:\Program Files\Git\cmd\git.exe`.

## 5. Registro de testes

### JDK 17 local

- Distribuição: Eclipse Temurin Windows x64, `17.0.20.1+1`, obtida pela [API oficial Adoptium](https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse).
- SHA-256 conferido contra metadados oficiais: `E53A79C3C3D86865BD7E787903884331068E71321714FFD44F145785AFFC7CB0`.
- `android/.gradle/config.properties` define `java.home`; `android/.idea/gradle.xml` seleciona `GRADLE_LOCAL_JAVA_HOME`, conforme a [documentação Android](https://developer.android.com/build/jdks). Ambos são locais, ignorados pelo Git.
- Abrir a pasta **android** como projeto no Studio. A seleção configurada deve aparecer em Settings → Build, Execution, Deployment → Build Tools → Gradle. Wrapper 8.14.1 adicionado e `--version` validado; sincronização da IDE ainda não validada.
- Para abrir um terminal com esse JDK, executar `scripts\terminal-java17.cmd` a partir da raiz. `scripts\terminal-java17.cmd --check` validou `java` e `javac` na versão 17.0.20.1. JAVA_HOME/PATH mudam somente no terminal criado, sem alteração global. Foi usado CMD porque a política atual do PowerShell bloqueia scripts; essa política foi preservada.
- A instalação está em `.tools/`, não acompanha clones. Em outro notebook, instalar o JDK e ajustar os caminhos locais. Não confundir essa configuração com build Android aprovado.

A conexão ADB e as propriedades do POCO foram validadas em 16/09/2026. Testes do aplicativo continuam pendentes.

| Teste | Resultado | Evidência |
|---|---|---|
| B13 — Room no POCO | Aprovado em 16/09/2026 | 2 testes, 0 falhas/erros/ignorados. Insert/read, reabertura e isolamento de procedimentos; relatório em android/app/build/reports/androidTests/connected/debug/index.html. |
| Compilação APK debug + teste unitário | Aprovados em 16/09/2026 | BUILD SUCCESSFUL; 1 teste, 0 falhas/erros. APK em `android/app/build/outputs/apk/debug/app-debug.apk`. |
| Gradle/JDK e configuração Android | Aprovado em 16/09/2026 | Wrapper 8.14.1 com Temurin 17.0.20.1; tarefa `help` com BUILD SUCCESSFUL. Não equivale a APK compilado. |
| Conexão USB/ADB | Aprovada em 16/09/2026 | ADB 37.0.1; um dispositivo `device`; consultas de Android/API/build/ABI concluídas. |
| Instalação e abertura do APK StepWise | Aprovadas em 16/09/2026 | Após usuário habilitar instalação USB: install Success; abertura e reabertura Status ok; versão 0.1.0-mvp/build 1. |
| Permissões Android e tela | Estados conferidos no POCO | UI inicialmente não autorizada; depois três permissões granted=true e UI correspondente, preservadas após reabertura. Recusa/bloqueio permanente não exercitados. |
| Build offline com tela mínima | Aprovado em 16/09/2026 | assembleDebug e testDebugUnitTest --offline: sucesso, 1 teste aprovado. |
| CameraAccess / DAT com MDK | Aprovado no escopo B06/B07 | Exemplo oficial 0.9.0 instalado; 1 dispositivo mock pareado, sessão started → stopped; sem stream/mídia. MDK desabilitado ao encerrar. |
| Modelo visual no celular | Aprovado no escopo B20 smoke | 1 teste instrumentado no POCO Android 16: asset `.tflite` carregado via TensorFlow Lite Interpreter, entrada sintética 256x256 executada, saídas boxes/classes/scores/count presentes. Relatório em `android/app/build/reports/androidTests/connected/debug/index.html`. |
| Voz e TTS offline | Parcial aprovado no celular | Android TTS integrado a demo controlada; `assembleDebug`, `testDebugUnitTest`, `installDebug` e abertura no POCO aprovados em 17/09/2026. Usuario confirmou audio em todos os botoes da demo. Modo aviao e rota Bluetooth/Ray-Ban pendentes. |
| Câmera, microfone e áudio dos Ray-Ban Meta | Não executado | — |
| Latência, bateria e estabilidade | Não executado | — |

Ao executar um teste, registrar data, versão do app/modelo, aparelho, condições, resultado e caminho da evidência. Resultados de mock, câmera do celular e óculos reais devem permanecer identificados.

## 6. Como manter a documentação durante a execução

- **Roadmap:** tarefas, dependências, critérios de conclusão e indicação do próximo passo.
- **Este arquivo:** inventário, versões realmente instaladas e evidências de testes do ambiente.
- **Registro de decisões, quando houver escolhas implementadas:** motivos, alternativas e resultado dos experimentos; caminho previsto `docs/decisoes-final.md`.
- **Relatórios e logs:** evidências detalhadas geradas nos testes, referenciadas pelos documentos; não substituir resultados por descrições de intenção.
- **Documentos submetidos e e-mails:** manter como histórico. Atualizar a documentação de implementação e os materiais futuros, preservando os arquivos originais.

Registrar informações quando forem confirmadas e marcar tarefas completas somente após o critério de conclusão. Não é necessário criar um documento novo para cada conversa.

## 7. Histórico

| Atualização | Alteração |
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
| 16/09/2026 — B08 | Usuário confirmou login e Developer Mode ativado no Meta AI, acessado por Informações do app e cinco toques na versão. Roteiro offline concluído; óculos indisponíveis, registro/pareamento reais pendentes. |
| 16/09/2026 — B18/B19 | Ambiente Linux de treino validado com Model Maker; smoke COCO sintetico treinou/exportou `.tflite`. Artefato local ignorado pelo Git; proximo passo B20 no Android. |
| 16/09/2026 — B20 | `stepwise_b19_smoke.tflite` copiado para assets Android e validado no POCO com `TfliteSmokeInferenceTest`. APK debug SHA256 `C7DC5D469EFE05990FC895B9BED72E3D050CBE898CAFB87992CDCA52C8FCF802`; APK de teste SHA256 `2B33474DC3154FA275CE7A316DAEE4141FA14A31F9163E0D90EC6F51B836C332`. |
| 16/09/2026 — B17 | WSL, Plataforma de Máquina Virtual e Ubuntu instalados pelo instalador oficial. Após reboot e primeira inicialização, Ubuntu WSL2 ficou operacional. |
| 16/09/2026 — B06/B07 | Referência oficial fixada no commit 974e05c; CameraAccess compilado, instalado e sessão MDK testada. Meta AI 289.0.0.25.162 identificado. Build-Tools 35.0.0 adicionado pelo exemplo oficial. |
| 16/09/2026 — B13 | 2 testes instrumentados Room aprovados no POCO. Executor removeu APKs; StepWise reinstalado e aberto com sucesso. Permissões zeradas pela reinstalação. |
| 16/09/2026 — B11/B12/B14 | Tela mínima instalada no POCO; duas aberturas bem-sucedidas, permissões autorizadas refletidas na UI. Build offline e teste unitário aprovados. |
| 16/09/2026 — JDK 17 | Temurin instalado com checksum verificado; java/javac executados; configuração local da IDE e terminal preparada. API 36 também confirmada no disco. Gradle/build continuam pendentes. |
| 16/09/2026 — após instalação do Studio pelo usuário | Inspeção confirmou Studio, JBR 21.0.10, SDK com android-36.1, Build-Tools 36.0.0 e Platform-Tools 37.0.1. B03/B04 parciais; sem build ou alteração das versões do projeto. |
| 16/09/2026 — validação USB | ADB portátil instalado; POCO autorizado e propriedades lidas. A01, B01 e B05 concluídas. SDK completo e APK ainda pendentes. |
| 16/09/2026 — inspeção B01 | Notebook do evento inventariado; Git e JRE executados; Python 3.14.7 identificado. Android Studio, JDK e SDK/ADB anteriores não localizados. Presença física do POCO confirmada pelo usuário. |
| Inventário inicial informado pelo usuário | Registrados POCO X8 Pro Max, Android/build, HyperOS, RAM, espaço livre, cabo e Meta AI instalado. Disponibilidade física preservada como pendente devido à resposta ambígua. Nenhum teste físico declarado como concluído. |
