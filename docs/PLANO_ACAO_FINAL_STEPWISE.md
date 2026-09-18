# StepWise â€” plano de aÃ§Ã£o para a final

**Data-base:** 15/09/2026 Â· **Evento:** 18/09/2026 Â· **VersÃ£o:** 1.0

**Objetivo:** chegar com o aplicativo, a IA, o procedimento, o Ã¡udio, a persistÃªncia e a demonstraÃ§Ã£o preparados e testados; usar o evento para parear os Ray-Ban Meta, validar as rotas reais, calibrar a percepÃ§Ã£o e medir o conjunto.

**Estado deste arquivo:** plano em execuÃ§Ã£o. Em 16/09/2026, APK debug compilado, instalado e aberto no POCO; 1 teste unitÃ¡rio e build offline aprovados. Tela mÃ­nima e estado autorizado das permissÃµes conferidos. DAT resolvido como dependÃªncia; conexÃ£o com Ã³culos, IA e voz ainda nÃ£o validadas.

## Onde estamos e qual Ã© o prÃ³ximo passo

**ConcluÃ­do: A01, B01 e B05.** POCO X8 Pro Max conectado e autorizado: Android **16**, API **36**, build `BP2A.250605.031.A3` e ABI **`arm64-v8a`**, confirmados por ADB. HyperOS `3.0.303.0.WPLMIXM.C11`, RAM fÃ­sica de 12 GB, 146,3 GB livres e Meta AI instalado foram informados pelo usuÃ¡rio.

Notebook do evento: Lenovo `80YF`, aproximadamente 8 GB de RAM, Windows 11 Pro. SDK 36, Build-Tools 36.0.0, Gradle 8.14.1 e JDK 17 validados por compilaÃ§Ã£o. Studio mantÃ©m JBR 21 para a IDE; sua sincronizaÃ§Ã£o pela interface ainda nÃ£o foi observada. O [inventÃ¡rio do ambiente](ambiente-validado.md) contÃ©m caminhos e evidÃªncias.

**B02, B04 e B06–B20 concluídas.** CameraAccess e sessão simulada MDK testados; Room com 2 testes aprovados. B08: login e Developer Mode Meta AI confirmados pelo usuário, roteiro offline em [meta-ai-registro-real.md](meta-ai-registro-real.md). B15/B16: Python 3.10.11 x64 local e `.venv-tools` validados; Python 3.14 global preservado. B17/B18/B19: Ubuntu WSL2, Python 3.10.21 via `uv`, Model Maker e treino/exportação smoke `.tflite` validados; detalhes em [b17-b19-treino-exportacao.md](b17-b19-treino-exportacao.md). B20: POCO executou inferência sintética com TensorFlow Lite Interpreter e validou saídas de boxes/classes/scores/count. Em 17/09/2026, o kit real foi ajustado para uma montagem com torneira/conexões, e o dataset `dataset stepwise/` foi padronizado com 581 imagens. Manifesto e splits foram gerados em `ml/data/stepwise_real/`; detalhes em [dataset-real-stepwise.md](dataset-real-stepwise.md). Registro real, pareamento e vídeo dos óculos permanecem pendentes de hardware. Arquivos ainda sem commit. **E15–E17 concluídos tecnicamente:** primeiro classificador real treinado, exportado e validado no POCO com imagens reais. E20/E21 confirmaram que a qualidade atual é insuficiente para avanço automático. C01/C04/C06–C10 e D02–D06 foram implementados para decisão conservadora. Motor ligado à tela em modo demo/controlado e instalado no POCO. Próximo passo: preparar fonte local de imagens/diagnóstico ou iniciar áudio/TTS, antes do DAT real. B21/B22 continuam úteis para congelamento, mas não devem atrasar o refinamento do modelo real.

**Limite da validaÃ§Ã£o:** tela de preparaÃ§Ã£o e operaÃ§Ãµes bÃ¡sicas Room testadas; sessÃ£o DAT, detector, voz, migraÃ§Ãµes do banco, recuperaÃ§Ã£o apÃ³s morte do processo e acompanhamento da montagem ainda nÃ£o validados.

## NavegaÃ§Ã£o

**ExecuÃ§Ã£o acelerada:** seguir [execuÃ§Ã£o em blocos](execucao-em-blocos.md), preservando os critÃ©rios individuais abaixo.

- [1. EstratÃ©gia e critÃ©rios de prontidÃ£o](#1-estratÃ©gia-e-critÃ©rios-de-prontidÃ£o)
- [2. O que manter e o que mudar](#2-o-que-manter-e-o-que-mudar)
- [3. Stack e decisÃµes tÃ©cnicas](#3-stack-e-decisÃµes-tÃ©cnicas)
- [4. OrganizaÃ§Ã£o da execuÃ§Ã£o](#4-organizaÃ§Ã£o-da-execuÃ§Ã£o)
- [5. Fase A â€” kit, escopo e decisÃµes](#5-fase-a--kit-escopo-e-decisÃµes)
- [6. Fase B â€” ambiente e build](#6-fase-b--ambiente-e-build)
- [7. Fase C â€” contratos e procedimento](#7-fase-c--contratos-e-procedimento)
- [8. Fase D â€” motor, adaptaÃ§Ã£o e dados](#8-fase-d--motor-adaptaÃ§Ã£o-e-dados)
- [9. Fase E â€” visÃ£o e treinamento](#9-fase-e--visÃ£o-e-treinamento)
- [10. Fase F â€” Ã¡udio e comandos](#10-fase-f--Ã¡udio-e-comandos)
- [11. Fase G â€” DAT e fontes de imagem](#11-fase-g--dat-e-fontes-de-imagem)
- [12. Fase H â€” integraÃ§Ã£o e testes](#12-fase-h--integraÃ§Ã£o-e-testes)
- [13. Fase I â€” evidÃªncias e pacote de viagem](#13-fase-i--evidÃªncias-e-pacote-de-viagem)
- [14. Runbook do evento](#14-runbook-do-evento)
- [15. Planos B e limites](#15-planos-b-e-limites)
- [16. Checklist de saÃ­da e decisÃµes para mentoria](#16-checklist-de-saÃ­da-e-decisÃµes-para-mentoria)
- [17. Fontes e autoridade das decisÃµes](#17-fontes-e-autoridade-das-decisÃµes)

## 1. EstratÃ©gia e critÃ©rios de prontidÃ£o

### 1.1 O que precisa estar pronto antes do evento

1. APK instalÃ¡vel no celular-alvo, com modelos e voz disponÃ­veis sem download.
2. Seis etapas de um Ãºnico kit real descritas em uma configuraÃ§Ã£o canÃ´nica.
3. IA real executando no celular, com avaliaÃ§Ã£o em dados separados do treinamento.
4. Motor que distingue `CORRECT`, `KNOWN_ERROR` e `INCONCLUSIVE`.
5. InstruÃ§Ãµes por Ã¡udio e comandos em portuguÃªs, com botÃ£o de contingÃªncia.
6. Fading por etapa, histÃ³rico persistente e cenÃ¡rio demonstrativo identificÃ¡vel.
7. Adaptador DAT compilado e exercitado com Mock Device Kit (MDK).
8. Caminho integrado com imagens reais do kit, recebido por fonte de desenvolvimento, e pelo MDK.
9. Tela tÃ©cnica que identifica fonte de imagem, rota de Ã¡udio, modelo, etapa e falhas.
10. Roteiro de demo, vÃ­deo prÃ©vio identificado como teste sem Ã³culos, folha de privacidade e ferramentas de mediÃ§Ã£o.

**SÃ³ deve ficar para o hardware:** pareamento/autorizaÃ§Ãµes do dispositivo disponibilizado, confirmaÃ§Ã£o do microfone e alto-falantes reais, qualidade e atraso dos frames, coexistÃªncia HFP/cÃ¢mera, autonomia de bateria e calibraÃ§Ã£o final.

O MDK reduz o trabalho de integraÃ§Ã£o, mas nÃ£o prova Bluetooth, perspectiva Ã³ptica, bateria, latÃªncia de transmissÃ£o ou roteamento acÃºstico. NÃ£o prometer que o evento exigirÃ¡ apenas um clique.

### 1.2 Marcos de liberaÃ§Ã£o

| Marco | EvidÃªncia para considerar concluÃ­do |
|---|---|
| G0 â€” hardware e kit definidos | Celular identificado; seis estados fÃ­sicos fotografados; erros reproduzÃ­veis. |
| G1 â€” ambiente reproduzÃ­vel | APK compilado/instalado; exemplo DAT abre; IA de referÃªncia executa; versÃµes registradas. |
| G2 â€” nÃºcleo independente dos Ã³culos | Procedimento, motor, fading, Room e TTS funcionam com entradas controladas. |
| G3 â€” percepÃ§Ã£o real no celular | Modelo prÃ³prio ou contingÃªncia explicitada; teste com kit real; voz offline testada. |
| G4 â€” integraÃ§Ã£o prÃ©-evento | Mesmo controlador funciona com MDK e fonte local; falhas nÃ£o avanÃ§am etapa; cinco ciclos completos. |
| G5 â€” pacote congelado | APK, modelos, dependÃªncias, documentaÃ§Ã£o, backup e ensaio disponÃ­veis offline. |
| G6 â€” integraÃ§Ã£o real | Entrada e saÃ­da nos Ã³culos; IA real; caso correto, erro e inconclusivo; vÃ­deo gravado. |
| G7 â€” apresentaÃ§Ã£o | Fading audÃ­vel, medidas reais e cinco checkpoints demonstrÃ¡veis. |

**NÃ£o confundir:** frames gravados do kit processados por um modelo real sÃ£o teste de integraÃ§Ã£o; resultados predefinidos sÃ£o simulaÃ§Ã£o. Na demonstraÃ§Ã£o final, identificar a origem real de cada evidÃªncia.

## 2. O que manter e o que mudar

### 2.1 DiagnÃ³stico da base atual

O repositÃ³rio jÃ¡ expressa a arquitetura, mas os principais caminhos estÃ£o incompletos. `capturePhoto()` e o carregador de procedimentos lanÃ§am `NotImplementedError`; o detector retorna vazio; o Quality Gate sempre aceita; a fala estÃ¡ comentada; UI e controlador nÃ£o executam o fluxo. HÃ¡ estrutura Room e uma reduÃ§Ã£o bÃ¡sica de assistÃªncia, ainda sem integraÃ§Ã£o completa.

TambÃ©m nÃ£o foram encontrados Gradle Wrapper, modelo `.tflite` ou dataset anotado. O manifesto referencia `@mipmap/ic_launcher`, recurso ausente na Ã¡rvore examinada. Nesta sessÃ£o, `python` resolveu para **3.14.7**; `git`, `adb` e `gradle` nÃ£o estavam no PATH. Isso nÃ£o prova que estejam ausentes da mÃ¡quina; primeiro localizar instalaÃ§Ãµes existentes.

### 2.2 Matriz de reaproveitamento

| Arquivo/Ã¡rea | DecisÃ£o | AlteraÃ§Ã£o concreta planejada |
|---|---|---|
| `android/`, Kotlin, applicationId `com.stepwise` | Manter | Aplicativo nativo e estrutura simples. |
| `MainActivity.kt`, AppCompat | Completar | Uma tela com iniciar, verificar, ajuda, repetir, encerrar e diagnÃ³stico. XML/ViewBinding; sem migraÃ§Ã£o de UI necessÃ¡ria. |
| Gradle raiz/app/settings | Atualizar | Matriz compatÃ­vel, Wrapper, catÃ¡logo de versÃµes, DAT real, dependÃªncias fixas. |
| `minSdk = 26` | Rever | Adotar API 31 para a rota inicial baseada no exemplo DAT atual; verificar aparelho antes. |
| `.env.example` | Corrigir | Remover pressuposto de `META_DAT_API_KEY`; usar configuraÃ§Ã£o realmente exigida pelo SDK. `.env` nÃ£o Ã© lido automaticamente pelo Gradle. |
| `local.properties.example` | Atualizar | Caminho Windows vÃ¡lido do SDK; parÃ¢metros locais realmente utilizados. |
| `Procedure.kt`, JSON, loader | Manter conceito; reformular dados | VersÃ£o, regras espaciais, evidÃªncia, erros e instruÃ§Ãµes unificados ao kit real. |
| `ProceduralEngine.kt` | Reimplementar avaliaÃ§Ã£o | Motor puro, sem Android; resultado com motivo; estado e efeitos explÃ­citos. |
| `FadingPolicy.kt` | Ampliar | DomÃ­nio por etapa/repetiÃ§Ã£o; ajuda, erros repetidos e inconclusivo tratados corretamente. |
| `DetectionResult.kt` | Ampliar | Origem, tempo, qualidade e versÃ£o; coordenadas e rÃ³tulos com contrato Ãºnico. |
| `VisualDetector.kt` | Manter interface conceitual | Implementar modelo local real e uma contingÃªncia selecionÃ¡vel. |
| `MetaDatSessionManager.kt` | Substituir implementaÃ§Ã£o | Fonte de frames de stream; `capturePhoto()` somente como opÃ§Ã£o testada, nÃ£o dependÃªncia central. |
| `QualityGate.kt` | Implementar | AvaliaÃ§Ã£o e seleÃ§Ã£o de frame recente; incerteza e descarte explÃ­citos. |
| `VoiceInputManager.kt` | Substituir internamente | `AudioRecord` + reconhecimento offline; origem do Ã¡udio verificada. |
| `TtsManager.kt` | Completar | InicializaÃ§Ã£o, pt-BR offline, fila, conclusÃ£o e roteamento; Ã¡udio prÃ©-gerado de reserva. |
| `SessionController.kt` | Implementar | Uma verificaÃ§Ã£o por vez; timeouts, cancelamento, estado persistente e resposta. |
| Room/DAO existentes | Aproveitar conceitos | Aprendiz pseudÃ´nimo, sessÃ£o, tentativas e domÃ­nio por etapa; transaÃ§Ãµes e exclusÃ£o. |
| `LinkStateManager.kt` | Ampliar | ReconexÃ£o limitada e revalidaÃ§Ã£o sem premiar acerto duplicado. |
| `ml/train.py`, `export_litert.py` | Reescrever | Treino/exportaÃ§Ã£o reais, avaliaÃ§Ã£o e manifestos de modelos. |
| `ml/requirements.txt` | Substituir | Ambientes separados e versÃµes resolvidas; remover combinaÃ§Ã£o imposta de TF 2.16 com Model Maker legado. |
| Teste atual | Manter e ampliar | Invariantes de decisÃ£o, fading, persistÃªncia, repetiÃ§Ã£o e desconexÃ£o. |
| Mermaid, README e especificaÃ§Ã£o | Atualizar depois das decisÃµes | Descrever o que roda; distinguir testes, planos e mediÃ§Ãµes. Preservar submissÃµes histÃ³ricas. |
| `.tmp_analysis/`, `.tmp_pdf_reader/` | Excluir do versionamento | SÃ£o auxiliares de leitura de documentos, nÃ£o dependÃªncias do produto. NÃ£o apagar outros arquivos ao limpar. |

### 2.3 O que fica fora desta preparaÃ§Ã£o

Backend obrigatÃ³rio, login remoto, dashboard educacional completo, mÃºltiplos procedimentos, Ã­ndice de autonomia validado, LLM para decidir avanÃ§o, aplicaÃ§Ã£o iOS, interface visual nas lentes, wake word prÃ³prio, controle por â€œHey Metaâ€, infraestrutura MLOps, Docker para o app Android e reescrita multiplataforma.

## 3. Stack e decisÃµes tÃ©cnicas

### 3.1 Stack proposta

| Camada | Escolha inicial | Motivo / condiÃ§Ã£o |
|---|---|---|
| App | Kotlin nativo, Android Studio, JDK 17 | Reutiliza a base e aproxima o cÃ³digo dos exemplos DAT. |
| Build | AGP 8.11.1, Gradle Wrapper 8.14.1, Kotlin 2.2.21 | ReferÃªncias do exemplo CameraAccess consultado; validar o conjunto no G1. [S1â€“S3] |
| Android | `compileSdk/targetSdk 36`; `minSdk 31` | Baseline do exemplo atual; Android 12+ recomendado. NÃ£o concluir que o e-mail â€œAndroid 10+â€ garante esta combinaÃ§Ã£o. [S2] |
| UI | AppCompat + XML/ViewBinding | Manter dependÃªncias jÃ¡ existentes que compilarem; nÃ£o copiar Compose sÃ³ por estar no sample. |
| AssÃ­ncrono | Coroutines, `StateFlow`, `Mutex` | Estado observÃ¡vel, cancelamento e exclusÃ£o de verificaÃ§Ãµes simultÃ¢neas. |
| Procedimento | JSON local; parser na camada Android, tipos Kotlin no domÃ­nio | Usar o parser jÃ¡ disponÃ­vel na plataforma ou um Ãºnico serializer fixado; evitar adicionar um framework de configuraÃ§Ã£o. |
| Banco | Room 2.8.4 + KSP 2.2.21-2.0.5 | Candidatos fixos; verificar geraÃ§Ã£o e transaÃ§Ãµes no G1/G2. [S13â€“S14] |
| DAT | `com.meta.wearable:mwdat-core:0.9.0`, `mwdat-camera:0.9.0`, `mwdat-mockdevice:0.9.0` | Artefatos oficiais; sem componente de display. [S4] |
| PercepÃ§Ã£o principal | MediaPipe Tasks Object Detector, candidato `tasks-vision:1.0.0` | VersÃ£o encontrada no exemplo oficial; exigir compatibilidade do modelo exportado. [S7â€“S8] |
| Modelo | Detector pequeno da famÃ­lia suportada pelo Model Maker | ComeÃ§ar com MobileNet e domÃ­nio fechado; nÃ£o treinar modelo grande do zero. |
| ContingÃªncia visual | OpenCV 4.x com HSV ou ArUco | Escolher uma opÃ§Ã£o conforme o kit; fixar patch apÃ³s teste. NÃ£o implementar duas contingÃªncias completas. [S15] |
| Voz principal | `AudioRecord` + Vosk Android 0.3.75 / JNA 5.18.1 | VersÃµes do exemplo oficial; vocabulÃ¡rio e desempenho em pt-BR precisam passar no teste. [S9â€“S10] |
| Modelo de voz | `vosk-model-small-pt-0.3` | Modelo pequeno disponÃ­vel; validar os trÃªs comandos, sem presumir boa precisÃ£o pelo nome. [S9] |
| Voz alternativa | Reconhecedor on-device do Android | SÃ³ se disponÃ­vel em pt-BR no aparelho e demonstradamente offline, com fonte correta. [S11] |
| SaÃ­da | Android TTS pt-BR offline + arquivos curtos de reserva | Roteamento pelos Ã³culos, resposta previsÃ­vel. |
| Python de ferramentas | Python 3.10 x64 em `.venv-tools` | PreparaÃ§Ã£o de imagens, validaÃ§Ã£o de configuraÃ§Ã£o, anotaÃ§Ãµes e mediÃ§Ãµes. |
| Python de treinamento | Linux/WSL2 Ubuntu 22.04, Python 3.10, `.venv-train` | Isolar dependÃªncias legadas do Model Maker e evitar usar o Python global 3.14. |
| AnotaÃ§Ã£o | CVAT com exportaÃ§Ã£o COCO | InstÃ¢ncia jÃ¡ disponÃ­vel; se usar serviÃ§o hospedado, enviar apenas dados de coleta autorizados. [S17] |
| EvidÃªncias | JSONL/CSV + scripts Python | Resumo local sem backend ou painel adicional. |
| Versionamento | Git, commits pequenos e tag de pacote testado | Permite voltar ao APK conhecido sem reconstruir Ã s pressas. |

NÃ£o instalar MediaPipe e outro runtime LiteRT independente â€œpor garantiaâ€. Primeiro usar o runtime que consome o modelo escolhido; runtime adicional sÃ³ se necessÃ¡rio e comprovado. NÃ£o deixar `LATEST`, `+` ou `latest.release` nas dependÃªncias finais.

**Model Maker:** a documentaÃ§Ã£o oficial indica que nÃ£o estÃ¡ mais em manutenÃ§Ã£o ativa. A tentativa inicial usa a versÃ£o publicada `0.2.1.4`, ambiente isolado e tempo limitado. A Ã¡rvore oficial lista TensorFlow `<2.16` e NumPy `<2`; o repositÃ³rio atual impÃµe TF `>=2.16`, portanto nÃ£o Ã© uma base confiÃ¡vel para esse caminho. As dependÃªncias efetivas do pacote precisam ser resolvidas e testadas, sem `--no-deps`. [S5â€“S6]

**DAT:** o README bruto oficial consultado informa distribuiÃ§Ã£o por Maven Central sem token para baixar os artefatos. HÃ¡ versÃµes antigas/indexadas que citam GitHub Packages. Usar a origem que corresponder Ã  revisÃ£o fixada; nÃ£o inventar uma chave `META_DAT_API_KEY`. Credenciais de registro/release do app sÃ£o outro assunto. [S4]

### 3.2 DecisÃµes de comportamento

1. **Fonte principal:** frame do stream DAT selecionado por qualidade, solicitado apÃ³s `VERIFY`.
2. **CÃ¢mera inicial:** janela sob demanda a 7 fps como configuraÃ§Ã£o candidata; comparar 2 fps persistente somente se a reabertura for lenta.
3. **Ãudio inicial:** rota HFP estabelecida antes da cÃ¢mera e mantida estÃ¡vel durante a sessÃ£o. Reconhecimento suspenso durante TTS e decisÃ£o; manter rota nÃ£o significa processar fala permanentemente.
4. **Entrada:** trÃªs comandos, `VERIFY`, `HELP`, `REPEAT`. Sem hotword obrigatÃ³rio.
5. **IA visual:** principal treinada; contingÃªncia visual determinÃ­stica preparada antes do evento.
6. **Fading:** por aprendiz pseudÃ´nimo + procedimento/versionamento + etapa; evidÃªncia acumulada em repetiÃ§Ãµes independentes da mesma etapa.
7. **Falha tÃ©cnica:** resultado tÃ©cnico/inconclusivo; nunca um erro do aprendiz.
8. **Modo demonstraÃ§Ã£o:** histÃ³rico semeado identificado como tal; visÃ£o, decisÃ£o, persistÃªncia e mudanÃ§a de nÃ­vel continuam reais.

### 3.3 Arquitetura para trocar apenas a origem no evento

```mermaid
flowchart LR
  UI[BotÃµes de contingÃªncia] --> CTRL[Controlador de sessÃ£o]
  VOZ[AudioRecord + voz offline] --> CTRL
  LOCAL[Frames locais de teste] --> SOURCE[Contrato de fonte de frames]
  MDK[DAT com Mock Device Kit] --> SOURCE
  REAL[DAT com Ã³culos reais] --> SOURCE
  CTRL --> SOURCE
  SOURCE --> Q[SeleÃ§Ã£o e qualidade]
  Q --> IA[Detector real / contingÃªncia identificada]
  IA --> ENG[Motor procedural + fading]
  PROC[Procedimento versionado] --> ENG
  ENG --> DB[Room: estado e eventos]
  DB --> CTRL
  ENG --> AUDIO[TTS / Ã¡udio local]
  AUDIO --> OUT[SaÃ­da validada: telefone em teste ou Ã³culos]
```

O adaptador DAT usado com MDK deve ser o mesmo usado com hardware. A fonte local serve para desenvolver e diagnosticar, nÃ£o deve substituir silenciosamente o DAT quando a conexÃ£o real falhar.

## 4. OrganizaÃ§Ã£o da execuÃ§Ã£o

### 4.1 Frentes humanas

| Frente | ResponsÃ¡vel a preencher | Propriedade principal |
|---|---|---|
| A â€” Android/hardware | ______ | Build, DAT, rotas de Ã¡udio, lifecycle e instalaÃ§Ã£o. |
| B â€” percepÃ§Ã£o | ______ | Kit visual, coleta, anotaÃ§Ãµes, modelo, qualidade e contingÃªncia. |
| C â€” procedural/demo | ______ | Procedimento, motor, Room, fading, mÃ©tricas e apresentaÃ§Ã£o. |

Isso Ã© divisÃ£o de trabalho da equipe, nÃ£o pressupÃµe agentes automÃ¡ticos. Um responsÃ¡vel por arquivo compartilhado, integraÃ§Ã£o frequente e contrato combinado antes de implementar em paralelo.

### 4.2 Como executar cada tarefa

- Marcar `[x]` somente com a evidÃªncia indicada.
- Cada linha Ã© uma unidade de execuÃ§Ã£o; a fase Ã© apenas um agrupamento.
- Trabalhar em blocos de aproximadamente 15â€“60 minutos. Se uma linha levar mais de 90 minutos, dividir em subitens com resultado verificÃ¡vel.
- Registrar bloqueios como `ID / sintoma / tentativa / prÃ³xima aÃ§Ã£o / responsÃ¡vel`.
- Ao atingir o limite de investigaÃ§Ã£o, aplicar o plano B predefinido; nÃ£o consumir a vÃ©spera procurando combinaÃ§Ãµes de bibliotecas.
- Integrar ao fim de cada marco. â€œMeu mÃ³dulo funciona isoladoâ€ nÃ£o conclui G4.
- Estimativas sÃ£o planejamento, nÃ£o promessa: reservar cerca de **45â€“65 horas-pessoa**, mais downloads/treino e eventual coleta adicional. Reavaliar apÃ³s G1; trÃªs pessoas e dois dias Ãºteis exigem escopo disciplinado.

### 4.3 Caminhos crÃ­ticos

```text
A (kit/escopo) -> C (contratos) -> D (motor/dados) -----------+
B (ambiente) -> E (modelo exportado e testado no celular) --+-> H -> I -> evento
B (ambiente) -> F (voz/Ã¡udio) ------------------------------+
B (ambiente) -> G (DAT/MDK) -------------------------------+
```

E e F tÃªm provas curtas antecipadas no ambiente. NÃ£o esperar terminar o motor para descobrir que o modelo nÃ£o carrega ou que o comando nÃ£o existe no vocabulÃ¡rio.

### 4.4 Agenda de referÃªncia, ajustÃ¡vel Ã  disponibilidade

| Janela | A â€” Android | B â€” percepÃ§Ã£o | C â€” procedural/demo | Marco |
|---|---|---|---|---|
| 15/09, preparaÃ§Ã£o | InventÃ¡rio, ferramentas, sample DAT | Kit, imagens iniciais, viabilidade de treino | Unificar etapas e mensagens | G0 e inÃ­cio G1 |
| 16/09, primeiro bloco | Build, MDK, TTS e prova de voz | Treino/exportaÃ§Ã£o mÃ­nimo, primeiras anotaÃ§Ãµes | Contratos, regras e fading | G1 e G2 parcial |
| 16/09, segundo bloco | Fonte de frames e rotas | Dataset e detector; contingÃªncia | Room e controlador | Primeiro loop no celular |
| 17/09, primeiro bloco | MDK + fluxo integrado | ValidaÃ§Ã£o e calibraÃ§Ã£o | Falhas, mÃ©tricas e demo | G3/G4 |
| 17/09, segundo bloco | InstalaÃ§Ã£o e teste offline | Congelar modelo/kit | Ensaio, privacidade e pacote | G5 |
| 18/09 | Runbook da seÃ§Ã£o 14 | Ajuste aos frames reais | MediÃ§Ã£o, vÃ­deo e pitch | G6/G7 |

Congelar funcionalidades antes da viagem. Se o tempo real for menor, usar a matriz de cortes da seÃ§Ã£o 15; nÃ£o declarar todos os marcos cumpridos por necessidade de calendÃ¡rio.

## 5. Fase A â€” kit, escopo e decisÃµes

**Entrada:** materiais da proposta. **ResponsÃ¡veis:** todos, C registra. **OrÃ§amento:** 60â€“120 minutos compartilhados. **SaÃ­da:** G0.

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [x] A01 | Registrar modelo do celular, Android/API, RAM, ABI, espaÃ§o livre, cabo e notebook. | ConcluÃ­da em 16/09/2026; inventÃ¡rio em `docs/ambiente-validado.md`, API 36 e ABI arm64-v8a confirmadas por ADB. |
| [ ] A02 | Identificar se hÃ¡ acesso prÃ©vio aos Ã³culos e qual kit fÃ­sico estÃ¡ disponÃ­vel. | Disponibilidade registrada; kit reservado para coleta e viagem. |
| [ ] A03 | Resolver a divergÃªncia sobre celular prÃ³prio versus fornecido. | ResponsÃ¡vel leva a questÃ£o Ã  organizaÃ§Ã£o; plano tÃ©cnico usa aparelho conhecido e prevÃª instalaÃ§Ã£o no fornecido. |
| [ ] A04 | Montar o kit manualmente e fotografar seis estados sucessivos. | Cada estado Ã© fisicamente realizÃ¡vel e distinguÃ­vel do anterior. |
| [ ] A05 | Comparar a sequÃªncia real com slides finais e especificaÃ§Ã£o. | Uma sequÃªncia `gear_v1` aprovada internamente; refinamento tÃ©cnico documentado, sem mudar problema/usuÃ¡rio. |
| [ ] A06 | Escolher trÃªs categorias observÃ¡veis de erro. | Uma foto por erro e descriÃ§Ã£o objetiva; preferÃªncia por peÃ§a errada, ausente e ordem/posiÃ§Ã£o inequÃ­voca. |
| [ ] A07 | Definir o que a cÃ¢mera nÃ£o comprova. | Torque, encaixe oculto, resistÃªncia e funcionamento interno excluÃ­dos das afirmaÃ§Ãµes de verificaÃ§Ã£o. |
| [ ] A08 | Escolher contingÃªncia visual: HSV se cores forem distintivas; ArUco se identidade/posiÃ§Ã£o forem ambÃ­guas. | Uma escolha, lista de peÃ§as/marcadores e materiais preparados. |
| [ ] A09 | Definir responsÃ¡veis A/B/C e limites de decisÃ£o. | Nome por frente e pessoa que decide simplificaÃ§Ãµes. |
| [ ] A10 | Criar registro de decisÃµes com data, motivo e evidÃªncia pendente. | `docs/decisoes-final.md` contÃ©m cÃ¢mera, Ã¡udio, voz, detector, fading e planos B. |

**Bloqueio real:** sem kit definitivo, coletar/treinar em um kit arbitrÃ¡rio nÃ£o resolve o maior risco. Ambiente, motor e MDK podem avanÃ§ar; percepÃ§Ã£o sÃ³ pode ser considerada validada no kit que irÃ¡ Ã  final.

## 6. Fase B â€” ambiente e build

**DependÃªncias:** A01 para o aparelho; nÃ£o depende da anotaÃ§Ã£o completa. **ResponsÃ¡veis:** A no Android, B no Python. **OrÃ§amento:** 3â€“5 horas-pessoa, com limites abaixo.

### B1. Ferramentas e primeiro APK

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [x] B01 | Localizar Git, Android Studio, JDK, SDK e platform-tools existentes antes de instalar. | ConcluÃ­da em 16/09/2026: Git e ADB executam pelos caminhos explÃ­citos registrados; ferramentas faltantes identificadas para B03/B04. |
| [x] B02 | Criar branch de preparaÃ§Ã£o e registrar o estado anterior. | Branch prep/final-2026-09-18 criada; commit de origem e arquivos preexistentes registrados, sem limpeza/reset. |
| [ ] B03 | Configurar Android Studio para o JDK 17 do projeto. | IDE e Gradle usam a mesma versÃ£o; `java -version` registrado. |
| [x] B04 | Instalar SDK Platform 36, Build Tools e platform-tools requeridos pelo build escolhido. | SDK 36 e Build-Tools 36.0.0 usados no APK; platform-tools 37.0.1 validado. |
| [x] B05 | Habilitar depuraÃ§Ã£o USB e autorizar o notebook no celular. | Validada em 16/09/2026: `adb devices -l` mostra `device`; propriedades do POCO lidas com sucesso. |
| [x] B06 | Baixar o repositÃ³rio oficial DAT para pasta de referÃªncia e registrar commit/tag. | Commit 974e05c569da8be01c0c9ce8bc5bc79b73ff8540 registrado; licenÃ§a preservada; CameraAccess abre. |
| [x] B07 | Compilar e instalar CameraAccess no celular-alvo. | App instalado; registro/dispositivo MDK e inÃ­cio/fim de sessÃ£o simulada testados; APK de diagnÃ³stico guardado. Stream e registro real pendentes. |
| [x] B08 | Instalar Meta AI e preparar conta, Developer Mode e documentaÃ§Ã£o offline de registro. | VersÃ£o via ADB; login e Developer Mode ativado confirmados pelo usuÃ¡rio; roteiro em docs/meta-ai-registro-real.md. Registro e pareamento reais dependem dos Ã³culos. |
| [x] B09 | Adicionar Gradle Wrapper completo e catÃ¡logo de versÃµes ao projeto StepWise. | Wrapper 8.14.1 e catÃ¡logo adicionados e usados no build; arquivos preparados para versionamento, sem commit ainda. |
| [x] B10 | Aplicar matriz de versÃµes e artefatos DAT corretos. | APK compilado com AGP/Kotlin/KSP/Room previstos e DAT 0.9.0 resolvido via Maven Central; sem chaves fictÃ­cias. |
| [x] B11 | Corrigir Ã­cone ausente, SDK path, manifesto e permissÃµes da versÃ£o adotada. | Build aprovado e fluxo runtime implementado; estados inicial/autorizaÃ§Ãµes refletidos no POCO. Recusa e bloqueio permanente ainda nÃ£o exercitados. |
| [x] B12 | Instalar StepWise com tela mÃ­nima. | InstalaÃ§Ã£o Success; duas aberturas Status ok, versÃ£o 0.1.0-mvp/build 1 visÃ­vel. Sem integraÃ§Ã£o funcional com Ã³culos. |
| [x] B13 | Compilar Room com KSP e executar insert/read mÃ­nimo em teste. | 2 testes instrumentados aprovados no POCO: insert/read, reabertura do banco e isolamento/limpeza por procedimento. |
| [x] B14 | Rodar build offline depois do primeiro build online. | Build incremental com alteraÃ§Ã£o de UI e teste passou em --offline em 16/09/2026. Vale para o cache deste notebook; modelos ainda nÃ£o integrados. |

Comandos de inspeÃ§Ã£o, no PowerShell da raiz:

```powershell
Get-Command git, java, adb, python -ErrorAction SilentlyContinue
java -version
python --version
adb devices -l
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.cpu.abi
```

Depois de B09, a partir de `android/`:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
.\gradlew.bat :app:assembleDebug --offline
```

NÃ£o executar esses comandos esperando que o Wrapper jÃ¡ exista na base atual. Em `local.properties`, usar o caminho real instalado, por exemplo `sdk.dir=C:/Users/user/AppData/Local/Android/Sdk`, verificando a pasta antes.

### B2. Python: ferramentas separadas do treinamento

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [x] B15 | Instalar/localizar Python 3.10 x64, preservando o Python global. | Python 3.10.11 x64 em `.tools/python310/python.exe`; global 3.14.7 preservado. |
| [x] B16 | Criar `.venv-tools` para preparaÃ§Ã£o de imagens/configuraÃ§Ãµes. | Imports aprovados, `pip check` sem conflitos e PNG OpenCV/Pillow validado. |
| [x] B17 | Preparar Linux Python 3.10 para treino: WSL2 Ubuntu 22.04 ou Linux jÃ¡ disponÃ­vel. | Ubuntu WSL2 rodando; Python 3.10.21 instalado via `uv`; venv de treino em `~/.venvs/stepwise-train`. |
| [x] B18 | Criar `.venv-train` e tentar instalar Model Maker 0.2.1.4 com restriÃ§Ãµes compatÃ­veis. | `pip check` e import de `object_detector` passaram; lock em `ml/requirements-train.lock.txt`. |
| [x] B19 | Executar exemplo oficial mÃ­nimo de treino e exportaÃ§Ã£o. | Smoke COCO sintÃ©tico treinou 1 Ã©poca/1 passo e exportou `ml/models/b19_smoke/stepwise_b19_smoke.tflite`; nÃ£o Ã© modelo do kit real. |
| [x] B20 | Carregar esse modelo no app e inferir uma imagem de exemplo. | `TfliteSmokeInferenceTest` passou no POCO; asset `models/stepwise_b19_smoke.tflite` carregado, tensores alocados e inferÃªncia sintÃ©tica validou boxes/classes/scores/count. |
| [ ] B21 | Congelar ambientes que passaram. | Arquivos lock, versÃ£o Python, sistema, comando e hashes dos modelos registrados. |
| [ ] B22 | Testar recriaÃ§Ã£o do ambiente por outro integrante ou em venv limpo. | InstalaÃ§Ã£o pelo lock e import reproduzÃ­veis; pacote de viagem nÃ£o depende de novo treino. |

**Ferramentas Windows â€” recriaÃ§Ã£o**, usando o runtime local instalado em B15:

```powershell
& .\.tools\python310\python.exe -m venv .venv-tools
.\.venv-tools\Scripts\python.exe -m pip install -r ml/requirements-tools.lock.txt
.\.venv-tools\Scripts\python.exe -m pip check
.\.venv-tools\Scripts\python.exe -c "import cv2, numpy, PIL; print(cv2.__version__, numpy.__version__)"
```

As faixas iniciais estÃ£o em `ml/requirements-tools.txt`; o lock registra as versÃµes instaladas. Usar somente `opencv-contrib-python` neste ambiente. O runtime local nÃ£o foi registrado como `py -3.10`; usar seu caminho explÃ­cito. A recriaÃ§Ã£o pelo lock ainda serÃ¡ validada em B22.

**Treinamento Linux â€” bootstrap candidato**, executado no terminal Linux, em checkout/cÃ³pia de trabalho prÃ³pria:

```bash
python3.10 -m venv .venv-train
.venv-train/bin/python -m pip install --upgrade pip
.venv-train/bin/python -m pip install "mediapipe-model-maker==0.2.1.4" "tensorflow<2.16" "numpy<2"
.venv-train/bin/python -m pip check
.venv-train/bin/python -c "from mediapipe_model_maker import object_detector; import tensorflow as tf; print(tf.__version__)"
.venv-train/bin/python -m pip freeze > ml/requirements-train.lock.txt
```

Congelar somente apÃ³s o treino/exportaÃ§Ã£o funcionar. NÃ£o forÃ§ar versÃµes removendo dependÃªncias. NÃ£o instalar CUDA no caminho crÃ­tico se CPU executar o ensaio em tempo aceitÃ¡vel. Se GPU for necessÃ¡ria, usar ambiente Linux/GPU jÃ¡ preparado; suporte TensorFlow em Windows nativo tem limitaÃ§Ãµes documentadas. [S16]

Se B18/B19 bloquear: usar ambiente de treino previamente validado de outro integrante ou um ambiente remoto controlado com Python compatÃ­vel. Colab atual nÃ£o Ã© automaticamente compatÃ­vel com dependÃªncias antigas. Limitar mais uma tentativa a 60 minutos e ativar a contingÃªncia da seÃ§Ã£o 15, enquanto o desenvolvimento Android continua.

### B3. Higiene mÃ­nima e instalaÃ§Ã£o offline

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] B23 | Ignorar venvs, pastas temporÃ¡rias de leitura, referÃªncias clonadas, mÃ­dia de coleta e bundles locais. | `.gitignore` cobre caminhos novos; cÃ³digo/configuraÃ§Ã£o e manifestos continuam versionados. |
| [ ] B24 | Definir onde guardar dataset/modelos grandes e backup. | Arquivos recuperÃ¡veis em duas cÃ³pias; hashes e caminhos registrados, sem depender apenas do Git. |
| [ ] B25 | Separar segredos locais de configuraÃ§Ã£o pÃºblica. | Nenhum token em APK/log; credenciais de download e registro nÃ£o confundidas. |
| [ ] B26 | Manter `docs/ambiente-validado.md` com matriz efetivamente testada. **Registro inicial criado; validaÃ§Ã£o pendente.** | VersÃµes propostas substituÃ­das pelas resolvidas, com modelo do celular, data e evidÃªncia de teste. |

**G1:** sample DAT e StepWise compilam; modelo de ensaio executa no telefone; ambiente de voz pode ser instalado; toolchain e dependÃªncias estÃ£o fixados.

## 7. Fase C â€” contratos e procedimento

**DependÃªncias:** A04â€“A08. **ResponsÃ¡vel:** C, revisÃ£o A/B. **OrÃ§amento:** 2â€“3 horas-pessoa.

### C1. Contratos mÃ­nimos entre mÃ³dulos

Estes nomes sÃ£o contratos propostos para cÃ³digo StepWise, nÃ£o nomes de APIs garantidos do DAT:

| Contrato | Dados/aÃ§Ãµes mÃ­nimos |
|---|---|
| `FrameSource` | Abrir janela, obter candidato recente, cancelar e fechar; estado da fonte. |
| `FrameEnvelope` | ID, origem (`LOCAL_TEST`, `DAT_MOCK`, `DAT_REAL`), tempos de captura quando disponÃ­veis e recebimento, dimensÃµes, rotaÃ§Ã£o, buffer/liberaÃ§Ã£o. |
| `QualityAssessment` | UtilizÃ¡vel, motivo, nitidez/exposiÃ§Ã£o, visibilidade da regiÃ£o, idade da evidÃªncia. |
| `VisionEvidence` | DetecÃ§Ãµes, qualidade, versÃ£o do modelo, duraÃ§Ã£o, ambiguidades. |
| `ProcedureDecision` | Tipo, motivo, erro conhecido opcional, etapa, frame/tentativa e mensagem. |
| `AudioInput` | Abrir rota, origem efetiva, iniciar/parar captura e liberar. |
| `CommandRecognizer` | PCM para `VERIFY/HELP/REPEAT/UNKNOWN`, com rejeiÃ§Ã£o de fala nÃ£o reconhecida. |
| `AudioOutput` | Falar/tocar, avisar inÃ­cio/fim/erro, confirmar rota e cancelar. |
| `SessionRepository` | Ler/salvar transaÃ§Ã£o, recuperar checkpoint, registrar tentativa e apagar aprendiz. |

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [x] C01 | Definir IDs, enums e motivos de erro técnico. | `StepResult`, `DecisionReason` e `EngineDecision` adicionados; avanço não depende de texto livre. |
| [ ] C02 | Especificar coordenadas normalizadas, orientaÃ§Ã£o e formato de cor. | Uma imagem de referÃªncia produz a mesma caixa em teste e Android. |
| [ ] C03 | Especificar tempos e propriedade dos buffers. | Consumidor sabe quando liberar; ninguÃ©m reutiliza buffer jÃ¡ fechado. |
| [x] C04 | Definir retorno da decisão com tipo/motivo. | `EngineDecision` retorna resultado, motivo, mensagem, etapa, classe observada e confiança. |
| [ ] C05 | Congelar interfaces pequenas para A/B/C. | Um exemplo de entrada e saÃ­da por interface; alteraÃ§Ãµes posteriores combinadas. |

### C2. Procedimento canÃ´nico

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [x] C06 | Atualizar `montagem_engrenagens.json` com ID e versão do procedimento real. | Asset atualizado para `montagem_torneira_stepwise_v1`, seis etapas reais da torneira/conexões. |
| [x] C07 | Descrever classes, quantidades e regiões esperadas de cada etapa. | Cada etapa tem classe esperada `S1`–`S6`; decisão conservadora exige confiança mínima. Regiões seguem pendentes para fonte real dos óculos. |
| [x] C08 | Definir condições dos três erros e em quais etapas se aplicam. | Erros `E1`–`E4` mapeados por etapa no procedimento; baixa confiança continua `INCONCLUSIVE`. |
| [x] C09 | Escrever mensagens L4/L3/L2/L1 por etapa. | Mensagens L4–L1 adicionadas nas seis etapas reais. |
| [x] C10 | Escrever feedback de acerto, erro, inconclusivo, conclusão e desconexão. | Motor retorna mensagens curtas para acerto, erro conhecido e evidência insuficiente; desconexão permanece em integração DAT. |
| [ ] C11 | Implementar loader e validador do JSON. | Rejeita IDs duplicados, rÃ³tulos inexistentes, etapa sem regra e mensagens incompatÃ­veis. |
| [ ] C12 | Criar exemplos positivos, negativos e inconclusivos de cada regra. | Fixtures pequenas para testar motor sem cÃ¢mera/Android. |

**Importante:** os nomes de etapa da apresentaÃ§Ã£o final sÃ£o referÃªncia, mas a montagem concreta precisa ser mecanicamente possÃ­vel. Registrar qualquer ajuste de sequÃªncia e refletir nos materiais; nÃ£o inventar peÃ§as para encaixar no JSON antigo.

## 8. Fase D â€” motor, adaptaÃ§Ã£o e dados

**DependÃªncias:** C; persistÃªncia requer B13. **ResponsÃ¡vel:** C. **OrÃ§amento:** 5â€“7 horas-pessoa. **SaÃ­da:** G2.

### D1. DecisÃ£o e avanÃ§o

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [x] D01 | Implementar sessão com estados `IDLE/INSTRUCTING/WAITING/VERIFYING/RECOVERING/COMPLETED`. | Parcial pragmático: demo controlada exibe etapa atual, conclusão e bloqueia verificações após concluir; estados completos do controlador real ainda pendentes. |
| [x] D02 | Implementar regras de presença, quantidade e região. | Regra inicial por classe esperada + confiança mínima implementada; região fica pendente para frames reais/âncoras. |
| [x] D03 | Implementar os erros explicitamente observáveis. | Classe de erro conhecido com confiança alta retorna `KNOWN_ERROR`, mensagem e não avança. |
| [x] D04 | Implementar abstenção conservadora. | Imagem ruim, sem procedimento, sem detecção, baixa confiança ou classe inesperada retornam `INCONCLUSIVE`. |
| [x] D05 | Separar `evaluate` puro de aplicação/persistência dos efeitos. | `evaluateDecision` não altera estado; `applyResult` aplica avanço/fading. |
| [x] D06 | Implementar término da sexta etapa. | Índice limitado ao tamanho do procedimento; `isCompleted()` cobre fim sem sétima etapa. |
| [x] D07 | Diferenciar tentativa técnica de tentativa avaliável do aprendiz. | Demo diferencia `CORRECT`, `KNOWN_ERROR` e `INCONCLUSIVE`; inconclusivo não avança. Persistência detalhada fica para integração Room/controlador. |

### D2. Fading por etapa e repetiÃ§Ã£o

PolÃ­tica inicial:

- Chave de domÃ­nio: `learner_id + procedure_version + step_id`.
- Acerto independente: primeira tentativa **avaliÃ¡vel**, sem ajuda e sem correÃ§Ã£o prÃ©via naquela execuÃ§Ã£o da etapa.
- Dois acertos independentes em repetiÃ§Ãµes da mesma etapa reduzem um nÃ­vel e zeram a sequÃªncia.
- `HELP`: aumenta suporte atÃ© L4 e zera sequÃªncia.
- `REPEAT`: repete a instruÃ§Ã£o; registrar separadamente e, nesta polÃ­tica, nÃ£o contar a execuÃ§Ã£o como independente. NÃ£o aumentar nÃ­vel automaticamente.
- Primeiro erro: zera sequÃªncia. Segundo erro na mesma execuÃ§Ã£o da etapa: aumenta suporte atÃ© L4.
- Inconclusivo/falha tÃ©cnica: nÃ£o altera nÃ­vel, sequÃªncia ou contagem de erros do aprendiz.
- RevalidaÃ§Ã£o apÃ³s reconexÃ£o nÃ£o conta como nova evidÃªncia de domÃ­nio.

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] D08 | Implementar domÃ­nio por etapa, separado do cursor atual da sessÃ£o. | Acerto em S1 nÃ£o reduz suporte de S2. |
| [ ] D09 | Implementar acerto independente e sequÃªncia de duas repetiÃ§Ãµes. | Teste mostra L3â†’L2 na segunda execuÃ§Ã£o elegÃ­vel da mesma etapa. |
| [ ] D10 | Implementar HELP, REPEAT e erros repetidos. | Comportamentos acima cobertos por testes. |
| [ ] D11 | Preservar domÃ­nio ao reiniciar o app ou comeÃ§ar nova repetiÃ§Ã£o. | Nova sessÃ£o do mesmo aprendiz usa o nÃ­vel previamente aprendido. |
| [ ] D12 | Criar cenÃ¡rio demo com L3 e um acerto prÃ©vio na etapa escolhida. | HistÃ³rico semeado identificado; prÃ³ximo acerto real produz L2. |
| [ ] D13 | Tornar a mudanÃ§a audÃ­vel na repetiÃ§Ã£o seguinte dessa etapa. | InstruÃ§Ã£o L2 Ã© de fato menor; nÃ£o reduzir outra etapa apenas para a apresentaÃ§Ã£o. |

### D3. PersistÃªncia e revalidaÃ§Ã£o

Entidades mÃ­nimas: `Learner` pseudÃ´nimo, `Session`, `StepAttempt`, `StepMastery`. O procedimento vem de JSON versionado. Evitar armazenar mÃ­dia em qualquer entidade.

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] D14 | Criar schema com sessÃ£o, versÃ£o de procedimento e domÃ­nio por etapa. | Dois aprendizes nÃ£o compartilham progresso por terem o mesmo `procedureId`. |
| [ ] D15 | Salvar tentativa, cursor e fading em uma transaÃ§Ã£o. | Falha no banco nÃ£o produz avanÃ§o confirmado apenas em memÃ³ria. |
| [ ] D16 | Restaurar cursor, nÃ­vel, streak e contadores necessÃ¡rios. | Reabrir app mantÃ©m exatamente o checkpoint confirmado. |
| [ ] D17 | Marcar restauraÃ§Ã£o como pendente de revalidaÃ§Ã£o fÃ­sica. | Reconectar nÃ£o avanÃ§a e nÃ£o duplica acerto; exigir nova evidÃªncia. |
| [ ] D18 | Definir o que revalidar: Ãºltimo checkpoint confirmado e integridade visÃ­vel. | MudanÃ§a da montagem durante a desconexÃ£o impede continuaÃ§Ã£o automÃ¡tica. |
| [ ] D19 | Implementar exclusÃ£o do aprendiz/sessÃµes e acesso ao histÃ³rico. | Registros relacionados removidos; exports externos tratados separadamente. |
| [ ] D20 | Executar suÃ­te do motor/fading e teste Room de recuperaÃ§Ã£o. | Casos crÃ­ticos aprovados; relatÃ³rio salvo. |

**G2:** uma sessÃ£o completa pode ser conduzida por botÃµes e evidÃªncias controladas, com Ã¡udio real, estado persistente e adaptaÃ§Ã£o correta. Resultados controlados continuam explicitamente identificados como testes.

## 9. Fase E â€” visÃ£o e treinamento

**DependÃªncias:** A04â€“A08, C02/C06â€“C08 e B18â€“B20. **ResponsÃ¡vel:** B, apoio C na coleta/anotaÃ§Ã£o. **OrÃ§amento:** 8â€“12 horas-pessoa, alÃ©m do processamento de treino. **Prioridade:** comeÃ§ar cedo; Ã© o maior risco apontado pela coordenaÃ§Ã£o.

### E1. Protocolo de coleta

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] E01 | Fixar fundo/bancada, Ã¡rea Ãºtil, peÃ§as e posiÃ§Ã£o inicial. | Fotos de referÃªncia e inventÃ¡rio; mesmo kit viajarÃ¡. |
| [ ] E02 | Criar lista de rÃ³tulos e IDs estÃ¡veis. | Classes sÃ£o peÃ§as/Ã¢ncoras visÃ­veis; nÃ£o usar `parafuso_apertado` como se torque fosse observÃ¡vel. |
| [ ] E03 | Preparar captura simulando a cÃ¢mera dos Ã³culos. | Celular na testa, ultra-wide quando disponÃ­vel, retrato; preservar originais e registrar transformaÃ§Ãµes. |
| [ ] E04 | Capturar as seis etapas corretas em pelo menos trÃªs sessÃµes distintas. | Mudar operador/Ã¢ngulo/luz; `capture_session_id` em todas as imagens. |
| [ ] E05 | Capturar os trÃªs erros em seus checkpoints relevantes. | Erros reproduzÃ­veis e rotulados; incluir peÃ§a correta solta ao lado como confundidor. |
| [ ] E06 | Capturar casos de incerteza. | MÃ£o cobrindo, desfoque, recorte, reflexo, baixa luz e peÃ§a fora da regiÃ£o Ãºtil. |
| [ ] E07 | Revisar amostra de 30 imagens antes de ampliar coleta. | Classes distinguÃ­veis, resoluÃ§Ã£o Ãºtil e nomes corretos; corrigir protocolo cedo. |
| [ ] E08 | Completar lote inicial de aproximadamente 300â€“500 imagens Ãºteis. | Cobertura de estados/erros; quantidade Ã© alvo de planejamento, nÃ£o garantia de precisÃ£o. |
| [ ] E09 | Separar por sessÃ£o fÃ­sica de captura. | Treino/validaÃ§Ã£o/teste sem sessÃ£o compartilhada; proporÃ§Ã£o aproximada 70/15/15 quando viÃ¡vel. |

NÃ£o distribuir frames vizinhos do mesmo vÃ­deo entre treino e teste. Reservar sessÃµes inteiras; se a base for muito pequena, coletar sessÃµes adicionais antes de publicar mÃ©trica. A imagem de celular Ã© aproximaÃ§Ã£o, nÃ£o equivalente ao sensor real.

### E2. AnotaÃ§Ã£o e controle de qualidade

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] E10 | Criar projeto CVAT com os rÃ³tulos fixados. | ExportaÃ§Ã£o COCO de cinco imagens lida pelo pipeline antes de anotar tudo. |
| [ ] E11 | Anotar o lote mÃ­nimo e revisar caixas/classes. | Todas as peÃ§as relevantes visÃ­veis anotadas; inconsistÃªncias corrigidas. |
| [ ] E12 | Registrar verdade procedural alÃ©m das caixas. | CSV por imagem: sessÃ£o, etapa, estado correto/erro/inconclusivo, erro e condiÃ§Ãµes. |
| [ ] E13 | Rodar validador do dataset. | Arquivos existem, caixas dentro da imagem, IDs vÃ¡lidos, duplicatas e divisÃ£o verificados. |
| [ ] E14 | Revisar 10% das anotaÃ§Ãµes por outra pessoa. | Erros de rÃ³tulo/enquadramento corrigidos antes do treino. |

**Dados de treinamento versus uso do produto:** a coleta intencional cria um dataset persistente. Documentar autorizaÃ§Ã£o, finalidade, local e retenÃ§Ã£o separados. A promessa de descartar imagens durante a operaÃ§Ã£o nÃ£o significa que nunca haverÃ¡ dataset de desenvolvimento. Usar bancada sem terceiros identificÃ¡veis.

### E3. Treinar, exportar e executar cedo

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [x] E15 | Implementar treino real a partir de `ml/data/stepwise_real/splits/*.csv`, com seed, configuração e checkpoint. | Treino curto executado no WSL em 17/09/2026; checkpoint gerado para `stepwise_real_classifier_v1`. Qualidade ainda baixa, uso como validação técnica inicial. |
| [x] E16 | Exportar o classificador para `.tflite` pelo fluxo TensorFlow/Keras usado no treino. | `.tflite` exportado, validado localmente e empacotado em assets Android; SHA-256 `222DF1B55815B7E6ADD809E7AE0709C6522DB65F2F08BFAFF121397CDE7C8720`. |
| [x] E17 | Executar o primeiro modelo próprio em cinco imagens reais no celular. | Teste instrumentado no POCO executou 5 imagens reais do dataset, além dos smokes; 3 testes, 0 falhas, BUILD SUCCESSFUL em 17/09/2026. |
| [ ] E18 | Completar treino com o conjunto de classes e monitorar validaÃ§Ã£o. | Modelo selecionado por validaÃ§Ã£o; teste preservado para avaliaÃ§Ã£o. |
| [ ] E19 | Comparar modelo original e quantizado somente se houver necessidade de tamanho/latÃªncia. | Registrar precisÃ£o/latÃªncia de ambos; manter original se quantizaÃ§Ã£o piorar decisÃµes. |
| [x] E20 | Implementar `ml/evaluate.py` no nível procedural. | Avaliador criado e executado em `test`/`val`; modelo de 17 classes colapsou em `P2`, modelo procedural colapsou em `E3_peca_ausente`. |
| [x] E21 | Calibrar limiares na validação. | Limiar 0,35 resultou em 100% de abstenção nos modelos avaliados; decisão registrada: não usar esses modelos para avanço automático sem refinamento. |
| [ ] E22 | Avaliar uma vez o candidato no conjunto de teste separado. | Contagens e denominadores salvos, inclusive falhas; nÃ£o selecionar modelo pelo teste. |
| [ ] E23 | Gerar manifesto do modelo. | Nome, hash SHA-256, classes, entrada, normalizaÃ§Ã£o, saÃ­da, runtime, versÃ£o do dataset e limites conhecidos. |
| [x] E24 | Empacotar modelo e manifesto no app e em backup. | Modelo, labels, manifesto e 10 imagens de diagnostico foram empacotados em assets. `StepwiseImageClassifier` executa top-3 local via TensorFlow Lite; usuario confirmou retorno de resultados no POCO. Qualidade do modelo segue insuficiente para avanco automatico. |

**NÃ£o basta renomear um arquivo para `.tflite`.** Um modelo exportado por outro framework pode exigir pÃ³s-processamento e metadados diferentes. O teste Android de E17 Ã© obrigatÃ³rio antes de ampliar o treino. [S7]

### E4. Qualidade e seleÃ§Ã£o de frame

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] E25 | Calcular nitidez e exposiÃ§Ã£o da regiÃ£o de trabalho. | ParÃ¢metros calibrados em imagens boas/ruins do kit; nÃ£o usar um limiar copiado como universal. |
| [ ] E26 | Verificar enquadramento/Ã¢ncoras e visibilidade suficiente. | AusÃªncia de peÃ§a por oclusÃ£o nÃ£o vira automaticamente erro do aprendiz. |
| [ ] E27 | Implementar buffer limitado de candidatos recentes. | No mÃ¡ximo uma janela pequena em RAM; descartar antigos e evitar fila crescente. |
| [x] E28 | Escolher frame de melhor qualidade apos o pedido de verificacao. | Parcial para fonte local validado: usuario selecionou uma imagem/foto pelo seletor nativo no POCO; app classificou, enviou ao motor da tentativa atual e a demo teve saida esperada. Selecao automatica por qualidade ainda pendente para camera/stream. |
| [ ] E29 | Implementar timeout e mensagem de recaptura. | Sem frame adequado, informar incerteza; nunca devolver `CORRECT` por padrÃ£o. |
| [ ] E30 | Medir idade e atraso visÃ­vel da evidÃªncia. | Distinguir horÃ¡rio de recebimento de captura; nÃ£o assumir frame fresco sÃ³ porque acabou de chegar. |

Recebimento recente nÃ£o garante captura recente quando hÃ¡ atraso na transmissÃ£o. Usar timestamp de captura quando disponÃ­vel e coerente; confirmar atraso real com mudanÃ§a visual conhecida/cronÃ´metro na bancada. Limpar candidatos de tentativas anteriores e bloquear frames atrasados ou de outra sessÃ£o.

### E5. Uma contingÃªncia visual pronta

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [x] E31 | Implementar a contingencia escolhida em A08, primeiro offline. | Contingencia local no Android validada: assets, foto selecionada e camera do celular alimentam o mesmo classificador/motor. Usuario confirmou que `Tirar foto e verificar` funcionou corretamente no POCO. Ainda nao e uma contingencia deterministica HSV/ArUco. |
| [x] E32 | Levar a mesma logica ao Android. | Caminho Android criado e validado no POCO: imagem local de assets -> `StepwiseImageClassifier` -> top-1 -> `ProceduralEngine` conservador -> texto/TTS. Qualidade do modelo ainda impede avanco automatico confiavel. |
| [ ] E33 | Testar peÃ§as ausentes, soltas, cobertas e em posiÃ§Ã£o errada. | Marcador detectado nÃ£o Ã© tratado como prova de encaixe/torque; geometria condiciona decisÃ£o. |
| [ ] E34 | Comparar principal e contingÃªncia nos mesmos casos. | Escolha registrada por desempenho; modo ativo visÃ­vel em diagnÃ³stico/log. |

Para ArUco: calibrar tamanho dos marcadores, legibilidade Ã  distÃ¢ncia e Ã¢ncoras da base. RelaÃ§Ãµes no plano podem usar referÃªncia da base; profundidade/encaixe continuam fora da prova quando nÃ£o observÃ¡veis. Para HSV: testar luz e reflexos; usar Ã¡rea/posiÃ§Ã£o e nÃ£o sÃ³ cor encontrada no frame.

**G3 visual:** percepÃ§Ã£o executa no celular e distingue os casos definidos; limitaÃ§Ãµes registradas; contingÃªncia instalÃ¡vel. Um classificador genÃ©rico de objetos sem relaÃ§Ã£o com o kit sÃ³ conclui teste de runtime, nÃ£o esta fase.

## 10. Fase F â€” Ã¡udio e comandos

**DependÃªncias:** B12; pode avanÃ§ar antes do treino. **ResponsÃ¡vel:** A; C revisa mensagens. **OrÃ§amento:** 4â€“6 horas-pessoa.

### F1. SaÃ­da por Ã¡udio primeiro

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [x] F01 | Inicializar TTS e selecionar voz pt-BR que nao exija rede. | `TtsManager` inicializa Android TTS em `pt-BR`, com fallback para idioma padrao; app compilado, instalado e aberto no POCO. Usuario confirmou audio nos botoes da demo em 17/09/2026. Teste em modo aviao/Bluetooth ainda pendente. |
| [x] F02 | Implementar `speak` com callbacks de inicio, fim e falha. | `TtsManager.speak` aceita callbacks de inicio, fim e falha via `UtteranceProgressListener`; demo chama TTS nos botoes de acerto, erro, inconclusivo, ajuda, repetir e reset. |
| [ ] F03 | Definir fila para â€œVerificandoâ€ e resultado. | Resultado rÃ¡pido nÃ£o corta a confirmaÃ§Ã£o; nenhuma fila acumulada de mensagens obsoletas. |
| [ ] F04 | Preparar arquivos de Ã¡udio para mensagens essenciais. | Acerto, erro, inconclusivo, desconexÃ£o e conclusÃ£o disponÃ­veis sem TTS. |
| [x] F05 | Validar instrucoes L4/L3/L2/L1 ouvindo sem olhar para a tela. | Validacao inicial aprovada no POCO: usuario clicou em todos os botoes da demo controlada e todos reproduziram audio. Refinar clareza das mensagens L4/L3/L2/L1 em ciclo posterior. |

### F2. Reconhecimento offline e origem do Ã¡udio

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] F06 | Baixar modelo Vosk pt-BR e registrar licenÃ§a/hash. | Arquivos locais completos, sem download na primeira abertura do app. |
| [x] F07 | Verificar o vocabulario para "verificar", "ajuda" e "repetir". | Parser local reconhece verificar/confere/conferir, ajuda/ajudar e repetir/repete/novamente; testes unitarios cobrem vocabulario principal e variacoes curtas. Validacao com ASR real ainda pendente. |
| [ ] F08 | Testar reconhecimento dos trÃªs comandos com Ã¡udio simples. | Pelo menos dez amostras por comando; registrar reconhecimentos e rejeiÃ§Ãµes. |
| [x] F09 | Adicionar saida `UNKNOWN`, debounce e limiar/regras de aceitacao. | `VoiceCommand.UNKNOWN` implementado; fala desconhecida nao aciona captura, nao altera estado e nao avanca etapa. Debounce temporal do ASR real ainda pendente. |
| [x] F10 | Implementar `AudioRecord` com formato PCM compativel com o modelo. | `MicrophoneCaptureManager` captura janela curta de 3 segundos em PCM 16 kHz mono usando `VOICE_RECOGNITION`, calcula RMS e reporta bytes lidos na UI. Usuario confirmou no POCO que a captura concluiu com valores nao zerados. |
| [ ] F11 | Selecionar dispositivo de comunicaÃ§Ã£o e confirmar rota efetiva. | `setCommunicationDevice`/callbacks e `AudioRecord.getRoutedDevice()` verificados durante captura ativa. [S12] |
| [ ] F12 | Testar com headset Bluetooth disponÃ­vel antes do evento. | Celular distante, Ã¡udio captado no headset; registrar que isso nÃ£o comprova os Ray-Ban. |
| [ ] F13 | Repetir comandos com ruÃ­do representativo. | ConfusÃ£o entre comandos, falhas e falsos disparos registrados. |
| [ ] F14 | Suspender reconhecimento durante TTS e verificaÃ§Ã£o. | A prÃ³pria fala â€œdiga verificarâ€ nÃ£o dispara nova tentativa. |
| [x] F15 | Implementar botao Verificar/Ajuda/Repetir no companion. | Botoes manuais da demo acionam o controlador para verificar/acertar, erro conhecido, inconclusivo, ajuda, repetir e reset; origem ainda nao persistida no historico. |
| [ ] F16 | Implementar encerramento de captura/rota ao terminar sessÃ£o. | Microfone e recursos liberados; app nÃ£o continua ouvindo em estado encerrado. |

**Limite de investigaÃ§Ã£o de voz:** se F07/F08 falhar por vocabulÃ¡rio/qualidade, testar reconhecedor on-device disponÃ­vel no aparelho em pt-BR. NÃ£o trocar automaticamente para `SpeechRecognizer` padrÃ£o em nuvem. Sherpa/Porcupine sÃ³ entram se houver modelo pt-BR e integraÃ§Ã£o jÃ¡ comprovados; nÃ£o presumir suporte aos comandos pelo nome da biblioteca.

### F3. PolÃ­tica de coexistÃªncia com cÃ¢mera

ConfiguraÃ§Ã£o inicial a testar: estabelecer HFP, aguardar confirmaÃ§Ã£o de rota, iniciar cÃ¢mera quando necessÃ¡rio, sintetizar mensagens pela rota validada e manter HFP estÃ¡vel durante a sessÃ£o. NÃ£o fixar taxa de 8 kHz como universal: registrar formato negociado/real e adequar o reconhecedor.

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] F17 | Criar configuraÃ§Ã£o de estratÃ©gia de Ã¡udio. | `HFP_SESSION` e contingÃªncia explÃ­cita selecionÃ¡veis sem reescrever o motor. |
| [ ] F18 | Instrumentar disponibilidade, rota, captura ativa e TTS. | Log mostra qual dispositivo foi usado; falha nÃ£o muda silenciosamente para o microfone do celular. |
| [ ] F19 | Tratar perda de rota de Ã¡udio durante sessÃ£o. | Suspender fluxo, preservar estado e indicar falha no companion; nÃ£o declarar feedback entregue nos Ã³culos. |

Existem relatos pÃºblicos de conflitos entre HFP e cÃ¢mera tambÃ©m no Android. SÃ£o relatos de desenvolvedores, nÃ£o garantias de comportamento da versÃ£o/aparelho da equipe. A configuraÃ§Ã£o recomendada no e-mail precisa passar no teste fÃ­sico; frame do stream nÃ£o elimina necessariamente todo conflito de coexistÃªncia. [S18]

**G3 voz:** comandos e TTS funcionam offline no celular; Bluetooth preliminar testado se houver headset; origem/saÃ­da nos Ray-Ban ficam explicitamente pendentes atÃ© G6.

## 11. Fase G â€” DAT e fontes de imagem

**DependÃªncias:** B07/B10, C01â€“C05. **ResponsÃ¡vel:** A, B integra conversÃ£o/qualidade. **OrÃ§amento:** 4â€“6 horas-pessoa.

### G1. Fonte local para desenvolver sem hardware

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] G01 | Implementar `LocalFixtureFrameSource`. | Recebe uma imagem por tentativa com metadados corretos; disponÃ­vel sÃ³ em modo identificado. |
| [ ] G02 | Carregar casos do kit reais, nÃ£o detecÃ§Ãµes inventadas. | IA executa sobre os pixels; modo local nÃ£o preenche `CORRECT` automaticamente. |
| [ ] G03 | Adicionar fonte CameraX somente se necessÃ¡ria para ensaio ao vivo. | Mesma interface; cÃ¢mera do telefone marcada como teste; evitar trabalho extra se MDK + fixtures bastarem. |

### G2. Adaptador DAT

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] G04 | Implementar inicializaÃ§Ã£o/registro e pedidos de permissÃ£o seguindo a revisÃ£o fixada do sample. | Cancelamento/negaÃ§Ã£o visÃ­veis; sem mÃ©todos inventados a partir dos pseudocÃ³digos antigos. |
| [ ] G05 | Implementar seleÃ§Ã£o do dispositivo e observaÃ§Ã£o de conexÃ£o. | Dispositivo ausente gera estado claro; nenhum fallback automÃ¡tico oculto. |
| [ ] G06 | Implementar abrir/fechar `DeviceSession` e capacidade de cÃ¢mera. | Recursos encerrados em sucesso, erro e cancelamento. |
| [ ] G07 | Consumir o stream usando os tipos reais da versÃ£o 0.9 adotada. | Frame entregue ao contrato normalizado; captura de foto nÃ£o Ã© requisito. |
| [ ] G08 | Converter frame respeitando formato, strides e orientaÃ§Ã£o fornecidos. | Imagem de referÃªncia sem cores trocadas, espelhamento ou rotaÃ§Ã£o indevida. |
| [ ] G09 | Conectar stream ao seletor de qualidade. | Janela limitada; backpressure descarta excesso; callback nÃ£o trava UI. |
| [ ] G10 | Adicionar timeout de abertura/primeiro frame/verificaÃ§Ã£o. | Falha termina com feedback e limpeza; sem espera infinita. |
| [ ] G11 | Configurar polÃ­tica inicial de cÃ¢mera sob demanda. | CÃ¢mera fecha apÃ³s obter evidÃªncia, inclusive se detector/TTS falhar. |
| [ ] G12 | Preparar configuraÃ§Ã£o opcional de stream 2 fps durante etapa. | PolÃ­tica desativada por padrÃ£o; log mede tempo ativo e informa mudanÃ§a de privacidade. |
| [ ] G13 | Tratar desconexÃ£o, pausa, bateria/temperatura quando expostos pelo SDK. | Controlador recebe erro tipado e preserva checkpoint. |
| [ ] G14 | Implementar tentativa limitada de reconexÃ£o e revalidaÃ§Ã£o. | Duas tentativas com intervalo configurado, depois aÃ§Ã£o explÃ­cita; sem loop permanente. |

### G3. Mock Device Kit

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] G15 | Emparelhar mock e simular ligado, aberto e em uso conforme API da revisÃ£o. | Sample e StepWise enxergam estados; registrar versÃ£o do MDK. |
| [ ] G16 | Fornecer vÃ­deo/imagem do kit ao mock. | Frame percorrido pelo DAT contÃ©m mÃ­dia escolhida; conversÃ£o no formato aceito pela versÃ£o. |
| [ ] G17 | Exercitar o pipeline com mÃ­dia mock atÃ© decisÃ£o e Ã¡udio. | Mesmo adaptador a ser usado no real; origem `DAT_MOCK` nos eventos. |
| [ ] G18 | Simular permissÃ£o negada, pausa e desconexÃ£o. | Sem crash, avanÃ§o indevido ou perda de domÃ­nio. |
| [ ] G19 | Confirmar polÃ­tica de seleÃ§Ã£o de fonte no APK do evento. | Modo real nÃ£o se converte em mock sem escolha explÃ­cita; UI identifica modo ativo. |

O MDK oficial permite simular dispositivo e mÃ­dia; capacidades/formato variam por revisÃ£o. A pÃ¡gina consultada descreve fornecimento de vÃ­deos/imagens. NÃ£o depender de uma funÃ§Ã£o genÃ©rica â€œusar cÃ¢mera do celular no MDKâ€ sem verificar que existe na versÃ£o instalada. Se necessÃ¡rio, CameraX Ã© uma fonte de teste separada. [S19]

**Preparar FFmpeg apenas se necessÃ¡rio** para gerar mÃ­dia suportada pelo mock. Converter antes da viagem e guardar o arquivo jÃ¡ testado; nÃ£o copiar comando de aceleraÃ§Ã£o especÃ­fico de macOS para Windows.

## 12. Fase H â€” integraÃ§Ã£o e testes

**DependÃªncias:** G2 + G3 visual/voz + DAT mock. **ResponsÃ¡veis:** A/C integram, B valida percepÃ§Ã£o. **OrÃ§amento:** 5â€“7 horas-pessoa. **SaÃ­da:** G4.

### H1. Um loop inteiro

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] H01 | Conectar iniciar sessÃ£o â†’ carregar procedimento â†’ restaurar estado â†’ falar instruÃ§Ã£o. | UsuÃ¡rio chega a `WAITING` sem intervenÃ§Ã£o de depuraÃ§Ã£o. |
| [ ] H02 | Conectar VERIFY â†’ confirmaÃ§Ã£o imediata â†’ frame â†’ qualidade â†’ IA â†’ motor. | Uma etapa real avaliada no telefone pelo caminho completo. |
| [ ] H03 | Persistir resultado e tocar feedback/prÃ³xima orientaÃ§Ã£o. | ConfirmaÃ§Ã£o de avanÃ§o sÃ³ apÃ³s transaÃ§Ã£o; tela reflete estado salvo. |
| [ ] H04 | Garantir uma verificaÃ§Ã£o por vez. | Comandos duplicados, cliques rÃ¡pidos e callback atrasado nÃ£o duplicam avanÃ§o. |
| [ ] H05 | Identificar cada tentativa com sessÃ£o, etapa e geraÃ§Ã£o de conexÃ£o. | Resultado antigo descartado apÃ³s reinÃ­cio, cancelamento ou mudanÃ§a de etapa. |
| [ ] H06 | Fechar recursos em `finally`/lifecycle adequado. | Buffer/cÃ¢mera/AudioRecord nÃ£o ficam ativos apÃ³s erro, tÃ©rmino ou pausa. |
| [ ] H07 | Integrar HELP e REPEAT ao mesmo fluxo de mensagens/estado. | AÃ§Ã£o auditÃ¡vel; reconhecimento nÃ£o escuta a prÃ³pria saÃ­da. |
| [ ] H08 | Implementar modo foreground previsÃ­vel para a demo. | App em primeiro plano durante sessÃ£o; ao ir para background, pausa e restaura com seguranÃ§a. |
| [ ] H09 | Integrar seis etapas e conclusÃ£o. | Procedimento completo, reinÃ­cio de nova repetiÃ§Ã£o preservando domÃ­nio. |
| [ ] H10 | Rodar os quatro cenÃ¡rios de apresentaÃ§Ã£o pelo MDK. | Acerto, erro, inconclusivo e fading acontecem no mesmo APK. |

Evitar serviÃ§o permanente em background nesta versÃ£o. Se bloquear a tela/rodar em background virar requisito indispensÃ¡vel, tratar como tarefa separada com foreground service e permissÃµes adequadas; nÃ£o assumir que o Android manterÃ¡ cÃ¢mera/microfone indefinidamente.

### H2. Matriz de aceitaÃ§Ã£o

Os testes abaixo sÃ£o necessÃ¡rios porque falhas podem produzir avanÃ§o indevido, perda de estado ou uma demonstraÃ§Ã£o enganosa. NÃ£o criar dezenas de testes de simples getters.

| ID | Caso | Resultado esperado |
|---|---|---|
| [ ] H11 | Estado correto, evidÃªncia vÃ¡lida | `CORRECT`, uma transaÃ§Ã£o e um avanÃ§o. |
| [ ] H12 | Cada um dos trÃªs erros | `KNOWN_ERROR` apropriado, correÃ§Ã£o e nenhuma mudanÃ§a de etapa. |
| [ ] H13 | OclusÃ£o, blur e baixa confianÃ§a | `INCONCLUSIVE`, sem erro/penalizaÃ§Ã£o do aprendiz. |
| [ ] H14 | PeÃ§a fora da Ã¡rea ou solta perto do eixo | NÃ£o aprovar apenas porque a classe apareceu. |
| [ ] H15 | Frame antigo/repetido apÃ³s nova aÃ§Ã£o | Descartar/revalidar; nunca aprovar a nova etapa por evidÃªncia anterior. |
| [ ] H16 | Dois comandos VERIFY quase simultÃ¢neos | Um processamento; nenhum avanÃ§o duplo. |
| [ ] H17 | Queda durante inferÃªncia e retorno tardio | Resultado descartado; estado salvo preservado. |
| [ ] H18 | Fechar e reabrir app | Restaurar etapa/domÃ­nio; exigir revalidaÃ§Ã£o; nÃ£o contar novo acerto por recuperar. |
| [ ] H19 | HELP, REPEAT e erros repetidos | PolÃ­tica de D2 respeitada, com mensagens adequadas. |
| [ ] H20 | Duas execuÃ§Ãµes independentes da mesma etapa | L3â†’L2; outra etapa nÃ£o recebe domÃ­nio indevido. |
| [ ] H21 | Inconclusivo seguido de acerto | Tentativa tÃ©cnica nÃ£o penaliza fading. |
| [ ] H22 | Modelo ausente/corrompido | Falha explÃ­cita, sem decisÃ£o fabricada; backup manual selecionÃ¡vel. |
| [ ] H23 | TTS indisponÃ­vel | Mensagem essencial por Ã¡udio de reserva; falha de rota exposta. |
| [ ] H24 | Sem internet | VerificaÃ§Ã£o e comandos continuam; listar qualquer dependÃªncia externa remanescente. |
| [ ] H25 | Apagar aprendiz/histÃ³rico | SessÃµes, tentativas e domÃ­nio removidos; sem restauraÃ§Ã£o inesperada. |
| [ ] H26 | Cinco ciclos completos consecutivos no celular | Nenhum crash, travamento, vazamento evidente ou avanÃ§o indevido. |

Comandos, depois da implementaÃ§Ã£o, em `android/`:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:assembleDebug --offline
```

Testes instrumentados exigem telefone conectado. Registrar versÃµes/APK testados; nÃ£o apresentar resultado anterior como validaÃ§Ã£o de mudanÃ§as posteriores.

### H3. CritÃ©rios propostos de qualidade

- Zero avanÃ§os em testes intencionais de incerteza, erro ou evidÃªncia antiga.
- Zero avanÃ§o duplicado ao repetir comando/callback.
- Cinco execuÃ§Ãµes completas consecutivas estÃ¡veis antes da viagem e nova confirmaÃ§Ã£o com hardware.
- Medir latÃªncia sem transformar a meta inicial de 1,5 s em resultado alegado.
- Para visÃ£o, reportar contagens por classe de cenÃ¡rio e falso aceite; pequeno conjunto nÃ£o demonstra uma taxa populacional de erro.
- Para voz, avaliar os trÃªs comandos e fala negativa; botÃ£o disponÃ­vel se ruÃ­do impedir reconhecimento.

Um sistema que responde sempre `INCONCLUSIVE` satisfaz a proibiÃ§Ã£o de avanÃ§o indevido, mas **nÃ£o** satisfaz prontidÃ£o. TambÃ©m precisa aceitar os estados corretos e identificar os erros definidos com cobertura Ãºtil, registrada nos testes.

**Bateria mÃ­nima de avaliaÃ§Ã£o proposta para G4/G6**, a ajustar antes de executar e registrar se houver reduÃ§Ã£o por tempo:

| Grupo | Casos iniciais | CritÃ©rio de liberaÃ§Ã£o proposto |
|---|---|---|
| Estados corretos | 30 verificaÃ§Ãµes, cinco por etapa | Pelo menos 24 aceites corretos; falhas restantes explicitamente inconclusivas, sem erro inventado. |
| Erros conhecidos | Dez exemplos de cada categoria, em etapas aplicÃ¡veis | Nenhum aceite falso e pelo menos oito identificaÃ§Ãµes corretas por categoria; restantes inconclusivas. |
| Imagem inadequada | Dez casos distribuÃ­dos entre oclusÃ£o, blur e enquadramento | Nenhum avanÃ§o e nenhuma penalizaÃ§Ã£o. |
| Comandos | Dez falas por comando, mais dez falas negativas | Pelo menos oito reconhecimentos de cada comando e nenhum disparo nas negativas do conjunto. |
| Fluxo completo | Cinco execuÃ§Ãµes do procedimento | Nenhum avanÃ§o indevido, perda de checkpoint ou travamento. |

Esses sÃ£o **critÃ©rios internos sugeridos para a demonstraÃ§Ã£o**, nÃ£o exigÃªncias da coordenaÃ§Ã£o nem comprovaÃ§Ã£o estatÃ­stica. Registrar condiÃ§Ãµes, quantidade e limitaÃ§Ãµes. No evento, reutilizar a estrutura com evidÃªncia nova dos Ã³culos; nÃ£o misturar nÃºmeros de mock e hardware real. Se um critÃ©rio falhar, corrigir, aplicar contingÃªncia ou assumir o resultado, sem esconder as tentativas anteriores.

## 13. Fase I â€” evidÃªncias e pacote de viagem

**DependÃªncias:** pode comeÃ§ar em paralelo; fechar apÃ³s G4. **ResponsÃ¡vel:** C, apoio A/B. **OrÃ§amento:** 4â€“6 horas-pessoa. **SaÃ­da:** G5.

### I1. InstrumentaÃ§Ã£o mÃ­nima

Um evento por tentativa, sem mÃ­dia bruta:

```text
session_id, learner_id_pseudonimo, procedure_version, step_id, attempt_id,
source_mode, detector_mode, model_hash, audio_route, decision, reason,
assistance_before, assistance_after, help_requested, repeat_requested,
command_end_ms, frame_received_ms, frame_capture_ms_if_available,
inference_ms, engine_ms, first_result_audio_ms, complete_feedback_ms,
camera_active_ms, mic_capture_ms, hfp_route_ms, connection_generation
```

Tempos de duraÃ§Ã£o usam relÃ³gio monotÃ´nico no telefone. Timestamp de parede serve para situar o evento; nÃ£o calcular duraÃ§Ã£o misturando relÃ³gio do sensor e do celular sem conversÃ£o vÃ¡lida.

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] I01 | Implementar exportaÃ§Ã£o local JSONL/CSV sem imagens/Ã¡udio. | Arquivo legÃ­vel, sem segredos, nomes reais ou transcriÃ§Ãµes desnecessÃ¡rias. |
| [ ] I02 | Definir latÃªncia ponta a ponta e instrumentar. | Reportar comando concluÃ­doâ†’inÃ­cio do resultado audÃ­vel; separar o â€œVerificandoâ€. |
| [ ] I03 | Registrar tambÃ©m tempo atÃ© fim do feedback. | Mensagem rÃ¡pida de progresso nÃ£o mascara demora da resposta final. |
| [ ] I04 | Criar script de resumo de latÃªncia. | Mediana, pior caso, nÃºmero de tentativas, falhas/timeouts separados; P95 opcional com amostra indicada. |
| [ ] I05 | Criar tabela de verdade versus decisÃ£o para a visÃ£o. | Corretos, trÃªs erros e ruins separados; inconclusivos nÃ£o excluÃ­dos silenciosamente. |
| [ ] I06 | Criar ficha de bateria para celular e Ã³culos. | InÃ­cio/fim, duraÃ§Ã£o, carga conectada, brilho, polÃ­tica cÃ¢mera/HFP e quantidade de verificaÃ§Ãµes. |
| [ ] I07 | Fazer ensaio de dez minutos no celular. | MediÃ§Ã£o preliminar rotulada como sem Ã³culos; nada extrapolado para autonomia dos Ray-Ban. |

FÃ³rmulas para o resumo:

- **Falso aceite:** montagens incorretas aceitas como `CORRECT` / montagens incorretas avaliadas.
- **PrecisÃ£o de CORRECT:** estados realmente corretos / todas as decisÃµes `CORRECT`.
- **Cobertura:** tentativas com decisÃ£o conclusiva / total de tentativas visuais vÃ¡lidas para avaliaÃ§Ã£o.
- **Inconclusivos:** quantidade e motivos / total, explicitando o conjunto usado.
- **Consumo:** diferenÃ§a em pontos percentuais de bateria no intervalo; normalizar para 10 minutos apenas informando a duraÃ§Ã£o original. Preferir medir 10 minutos reais.

Registrar exemplos individuais e denominadores. Zero erros em dez casos nÃ£o Ã© garantia de 100% de precisÃ£o fora da demonstraÃ§Ã£o.

### I2. Privacidade executÃ¡vel e documentaÃ§Ã£o curta

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] I08 | Documentar dados de operaÃ§Ã£o, dataset e vÃ­deo de demo separadamente. | Finalidade, local, acesso e retenÃ§Ã£o definidos para cada um. |
| [ ] I09 | Implementar buffers efÃªmeros e fechar referÃªncias apÃ³s uso. | Mostrar trechos reais de descarte/liberaÃ§Ã£o; sem promessa de apagamento forense da RAM. |
| [ ] I10 | Verificar diretÃ³rios/cache/banco apÃ³s uma sessÃ£o. | Nenhuma mÃ­dia operacional persistida por padrÃ£o; modo de coleta isolado e explÃ­cito. |
| [ ] I11 | Revisar rede do app, voz e telemetria de SDKs. | Caminho local testado; configurar opt-out de analytics/crash do DAT quando aplicÃ¡vel. [S4] |
| [ ] I12 | Escrever `docs/privacidade-demo.md`, com no mÃ¡ximo uma pÃ¡gina de leitura. | Fluxo, retenÃ§Ã£o, acesso/exclusÃ£o e finalidade explicados; responsÃ¡vel registra base legal adequada ao piloto. |
| [ ] I13 | Testar acesso e exclusÃ£o do histÃ³rico pelo usuÃ¡rio. | DeleÃ§Ã£o real, inclusive domÃ­nio por etapa; destino de exports informado. |

O histÃ³rico de desempenho pode ser dado pessoal mesmo sem nome no registro. Para o piloto, manter participantes adultos conforme o escopo e nÃ£o reutilizar automaticamente o histÃ³rico para avaliaÃ§Ã£o escolar. A definiÃ§Ã£o da base legal Ã© especÃ­fica ao tratamento/contexto; consultar art. 7Âº e, se aplicÃ¡vel, art. 11/14, sem marcar â€œLGPD cumpridaâ€ sÃ³ por haver processamento local. [S20]

**Proposta operacional de retenÃ§Ã£o para decidir em I08:** sessÃ£o convidado apagÃ¡vel ao encerrar; dados identificÃ¡veis do piloto excluÃ­dos apÃ³s a finalidade de avaliaÃ§Ã£o definida com participantes; evidÃªncia agregada mantida separadamente. Informar um prazo concreto no documento final, nÃ£o deixar â€œpor tempo necessÃ¡rioâ€ como Ãºnica regra.

### I3. EvidÃªncia do problema e roteiro de pitch

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] I14 | Entrevistar um instrutor disponÃ­vel, com autorizaÃ§Ã£o para usar relato agregado. | Registrar turma, relaÃ§Ã£o aprendiz/instrutor, etapas tÃ­picas e espera; diferenciar estimativa e mediÃ§Ã£o. |
| [ ] I15 | Preparar quatro cenas de demo. | Acerto, erro conhecido, inconclusivo e fading, usando configuraÃ§Ãµes e kit idÃªnticos ao APK. |
| [ ] I16 | Preparar cenÃ¡rio de domÃ­nio prÃ©-carregado. | Tela/explicaÃ§Ã£o identifica histÃ³rico demonstrativo; transiÃ§Ã£o executada pelo motor real. |
| [ ] I17 | Ensaiar roteiro em 3 minutos, com versÃ£o de 5 minutos. | DuraÃ§Ã£o Ã© alvo interno; confirmar tempo oficial, nÃ£o presumir autorizaÃ§Ã£o. |
| [ ] I18 | Corrigir justificativa duplicada e argumentos de diferenciaÃ§Ã£o. | Bateria/privacidade justificam captura; autonomia e estado sÃ£o explicados com precisÃ£o. |
| [ ] I19 | Atualizar slides/diagrama para decisÃµes realmente implementadas. | NÃ£o prometer captura somente sob demanda se stream contÃ­nuo tiver sido escolhido; sem percentuais antigos nÃ£o comprovados. |
| [ ] I20 | Gravar demo prÃ©-evento e identificar fonte simulada/local. | Reserva de preparaÃ§Ã£o; nÃ£o substituir evidÃªncia de Ã³culos reais. |

Roteiro interno de trÃªs minutos:

1. **0:00â€“0:25:** aprendiz, bancada e problema observado.
2. **0:25â€“1:05:** instruÃ§Ã£o, execuÃ§Ã£o, entrada e resposta reais.
3. **1:05â€“1:35:** erro conhecido e imagem inconclusiva; mostrar nÃ£o avanÃ§o.
4. **1:35â€“2:15:** explicar histÃ³rico prÃ©-carregado; acerto atual; repetir a etapa e ouvir L2.
5. **2:15â€“2:45:** arquitetura, IA, privacidade e mediÃ§Ãµes reais.
6. **2:45â€“3:00:** limitaÃ§Ãµes atuais e prÃ³ximo passo educacional.

Adaptar Ã  duraÃ§Ã£o oficial. O microfone da apresentaÃ§Ã£o precisa captar a resposta dos Ã³culos de forma inteligÃ­vel; se usar monitor/espelhamento tÃ©cnico, informar que ele serve Ã  banca e nÃ£o Ã© uma tela nas lentes.

### I4. Pacote congelado

| ID | AÃ§Ã£o pequena | CritÃ©rio de conclusÃ£o |
|---|---|---|
| [ ] I21 | Gerar APK final de preparaÃ§Ã£o e registrar hash/commit. | InstalaÃ§Ã£o limpa e atualizaÃ§Ã£o preservando dados testadas. |
| [ ] I22 | Guardar APK atual e Ãºltimo APK estÃ¡vel. | Ambos instalÃ¡veis no celular-alvo; chave de assinatura consistente entre notebooks. |
| [ ] I23 | Empacotar modelos, vozes, mÃ­dia MDK e instruÃ§Ãµes de instalaÃ§Ã£o. | Nenhum download grande necessÃ¡rio no evento. |
| [ ] I24 | Repetir build offline e instalaÃ§Ã£o por cabo. | Outro integrante consegue executar com as instruÃ§Ãµes. |
| [ ] I25 | Preparar backup em segundo notebook/armazenamento local. | CÃ³digo, APK, modelos, ferramentas e apresentaÃ§Ã£o recuperÃ¡veis sem rede. |
| [ ] I26 | Conferir kit fÃ­sico e logÃ­stica. | PeÃ§as, reserva quando possÃ­vel, marcadores, cabos de dados, carregadores, adaptadores e documento com foto. |
| [ ] I27 | Criar tag local da versÃ£o aprovada apÃ³s revisÃ£o do estado Git. | ReferÃªncia recuperÃ¡vel; mudanÃ§as posteriores restritas a defeitos e integraÃ§Ã£o. |

Estrutura proposta do bundle local, fora do versionamento de mÃ­dia/dados pessoais:

```text
event-bundle/
  apk/                    # StepWise atual, anterior e sample DAT
  models/                 # visÃ£o e voz, hashes/licenÃ§as
  mock-media/             # arquivos jÃ¡ aceitos pelo MDK
  docs/                   # ambiente, instalaÃ§Ã£o, privacidade, runbook
  demo/                   # slides PDF e vÃ­deo prÃ©vio identificado
  checksums.txt
  build-info.txt          # commit, versÃµes, aparelho e comandos
```

**G5:** outra pessoa abre o pacote, instala o aplicativo e executa a demonstraÃ§Ã£o prÃ©-evento sem reconstruir ambiente ou baixar modelos.

## 14. Runbook do evento

### 14.1 Antes de consumir o tempo de desenvolvimento

- Chegar para credenciamento Ã s **08h**, com documento com foto.
- Local informado: Meta, Av. Brig. Faria Lima, 3732, Itaim Bibi, SÃ£o Paulo.
- Confirmar celular que serÃ¡ usado, versÃ£o dos Ã³culos/firmware, disponibilidade de carga e tempo oficial de pitch.
- O e-mail fala em **seis horas de desenvolvimento**. O relÃ³gio abaixo usa tempo Ãºtil desde a liberaÃ§Ã£o, nÃ£o inventa um horÃ¡rio fixo de inÃ­cio. Ajustar aos checkpoints da organizaÃ§Ã£o; o edital previa verificaÃ§Ãµes Ã s 15h e 16h e fechamento Ã s 17h30.
- Frente A controla o Ãºnico par de Ã³culos. B pede janelas curtas de coleta; C registra resultados e prepara demo.

### 14.2 Primeira hora: provar compatibilidade e obter frames reais

| Tempo Ãºtil | ResponsÃ¡vel | AÃ§Ã£o | EvidÃªncia / limite |
|---|---|---|---|
| 00â€“10 min | A | Registrar aparelho/firmware/carga e confirmar pareamento no Meta AI. | Ã“culos conectados e em uso; nÃ£o iniciar mudanÃ§as amplas de versÃ£o. |
| 10â€“20 min | A | Rodar sample DAT de diagnÃ³stico; permissÃµes, Developer Mode e primeiro frame. | Se falhar, investigar registro/permissÃµes/estado fÃ­sico antes do motor StepWise. |
| 20â€“30 min | A | Abrir StepWise, estabelecer HFP, testar frase e comando com celular afastado. | Entrada/saÃ­da realmente nos Ã³culos; registrar rota; confirmar no rosto, abertos e prontos. |
| 30â€“40 min | A+B | Abrir stream e medir primeiro frame, atraso visual e coexistÃªncia com Ã¡udio. | Se conflito, acionar Ã¡rvore da seÃ§Ã£o 15; nÃ£o gastar a hora inteira insistindo em `capturePhoto`. |
| 40â€“60 min | B+A | Coletar 50â€“100 frames distribuÃ­dos entre estados, erros e condiÃ§Ãµes ruins. | Modo de coleta explÃ­cito; reservar subconjunto sem ajuste; testar modelo e contingÃªncia. |

Se pareamento/compatibilidade impedir a coleta nessa hora, registrar o desvio, pedir mentoria tÃ©cnica e manter B/C trabalhando com as evidÃªncias locais. O cronograma Ã© alvo, nÃ£o justificativa para inventar que os testes ocorreram.

### 14.3 Horas seguintes

| Tempo Ãºtil | Meta | Trabalho concreto |
|---|---|---|
| 01:00â€“01:30 | Uma etapa completa real | VERIFY â†’ frame â†’ IA â†’ decisÃ£o â†’ Ã¡udio, com persistÃªncia. |
| 01:30â€“02:00 | Escolha de percepÃ§Ã£o | Ajustar apenas ROI/limiar com lote de calibraÃ§Ã£o; validar no lote reservado; usar contingÃªncia se necessÃ¡rio. |
| 02:00â€“02:30 | Erro e inconclusivo | Reproduzir ambos; comprovar que nÃ£o avanÃ§am; conferir restauraÃ§Ã£o. |
| 02:30â€“03:00 | Loop estÃ¡vel e vÃ­deo | Executar repetidamente e gravar ciclo real assim que funcionar. **Limite para G6: primeira metade.** |
| 03:00â€“03:30 | Fading e roteiro | C assume demo/pitch; carregar histÃ³rico demonstrativo e mostrar transiÃ§Ã£o real. |
| 03:30â€“04:00 | MediÃ§Ãµes | Rodar bloco de 10 minutos com cÃ¢mera/Ã¡udio escolhidos; medir latÃªncia e consumo dos dois dispositivos. |
| 04:00â€“04:30 | Robustez | ReconexÃ£o, reinÃ­cio, trÃªs erros, cinco ciclos quando duraÃ§Ã£o permitir. |
| 04:30â€“05:00 | Congelar candidato | Corrigir apenas bloqueadores; APK de reserva e vÃ­deo atualizados. |
| 05:00â€“05:30 | Ensaio | Rodar apresentaÃ§Ã£o inteira, resets, Ã¡udio para banca e cenÃ¡rio de erro. |
| 05:30â€“06:00 | PreparaÃ§Ã£o final | Baterias, kit, aplicativo no estado inicial, nÃºmeros e limitaÃ§Ãµes prontos. |

Gravar vÃ­deo em qualquer momento anterior em que o fluxo ficar pronto; nÃ£o esperar o horÃ¡rio da tabela. NÃ£o mudar biblioteca/modelo principal nos Ãºltimos 60 minutos sem defeito bloqueante e retorno garantido ao pacote anterior.

### 14.4 ComparaÃ§Ã£o de polÃ­ticas de cÃ¢mera/Ã¡udio

Medir no hardware, com a mesma montagem e roteiro:

| ConfiguraÃ§Ã£o | BenefÃ­cio pretendido | O que observar |
|---|---|---|
| HFP estÃ¡vel + cÃ¢mera sob demanda a 7 fps | Rotas estÃ¡veis e pouca cÃ¢mera ativa | Tempo de abertura, primeiro frame e consumo. |
| HFP estÃ¡vel + stream 2 fps durante etapa | Menos espera de reabertura | Idade do frame, coexistÃªncia, bateria e captura mais prolongada. |
| CÃ¢mera dos Ã³culos + botÃ£o no companion | ContingÃªncia de comando | MantÃ©m entrada visual real e IA; saÃ­da ainda precisa estar nos Ã³culos. |

Escolher pela estabilidade do loop e medidas. ConfiguraÃ§Ã£o 2 sÃ³ entra se a 1 tiver custo relevante; nÃ£o Ã© obrigatÃ³rio comparar todas se a principal jÃ¡ passar e o tempo for curto. Registrar a polÃ­tica efetiva na folha de privacidade e no pitch.

### 14.5 DiagnÃ³stico por sintoma

1. **Nem sample funciona:** pareamento, conta, Developer Mode, permissÃ£o, Ã³culos abertos/em uso, compatibilidade; levar Ã  mentoria.
2. **Sample funciona e StepWise nÃ£o:** comparar manifesto, registro, versÃ£o e lifecycle; nÃ£o retreinar detector.
3. **Frame chega, IA falha:** cor/rotaÃ§Ã£o/ROI, hash do modelo, rÃ³tulos, prÃ©-processamento; comparar imagem conhecida.
4. **VisÃ£o funciona, voz nÃ£o:** origem efetiva, formato PCM, vocabulÃ¡rio e ruÃ­do; botÃ£o mantÃ©m a verificaÃ§Ã£o.
5. **TTS fala no celular:** rota e estabilizaÃ§Ã£o; nÃ£o considerar checkpoint de saÃ­da cumprido.
6. **CÃ¢mera falha somente com HFP:** reproduzir com diagnÃ³stico simples; testar sequÃªncia/contingÃªncia de Ã¡udio controlada, com mentor. NÃ£o presumir que stream elimina o problema.
7. **SessÃ£o reconecta mas montagem mudou:** revalidar checkpoint; nunca continuar automaticamente sÃ³ porque Room tem um nÃºmero de etapa.

## 15. Planos B e limites

### 15.1 Matriz de contingÃªncia

| Problema | Prazo de investigaÃ§Ã£o sugerido | Alternativa preparada | Limite que permanece |
|---|---|---|---|
| Celular nÃ£o atende baseline DAT | Antes do treino/integraÃ§Ã£o | Outro Android 12+ testado; aparelho fornecido com pacote pronto | NÃ£o reduzir minSdk Ã  forÃ§a sem verificar SDK/APIs. |
| Ambiente Model Maker nÃ£o instala | 60 min + uma alternativa de 60 min | Ambiente compatÃ­vel jÃ¡ validado; percepÃ§Ã£o determinÃ­stica + IA funcional documentada | Registrar alteraÃ§Ã£o tÃ©cnica; nÃ£o fingir detector treinado. |
| Detector fraco no kit | AtÃ© fim do bloco de validaÃ§Ã£o prÃ©-evento | HSV/ArUco escolhido em A08, jÃ¡ no Android | NÃ£o comprova encaixe/torque oculto; checkpoint de IA continua obrigatÃ³rio. |
| Detector falha nos frames dos Ã³culos | 30 min de calibraÃ§Ã£o apÃ³s coleta | ContingÃªncia local testada com os mesmos frames | Lote de calibraÃ§Ã£o separado da avaliaÃ§Ã£o. |
| Vosk nÃ£o reconhece os termos | Prova inicial F07/F08 | ASR on-device pt-BR validado | Se nÃ£o houver, botÃ£o Ã© contingÃªncia de entrada, nÃ£o prova de IA. |
| Voz falha com ruÃ­do | 10â€“15 min no evento | BotÃ£o do companion, mantendo cÃ¢mera real | Identificar limitaÃ§Ã£o hands-free. |
| HFP conflita com cÃ¢mera | 15â€“20 min e mentoria | BotÃ£o, cÃ¢mera com mic inativo e reproduÃ§Ã£o apÃ³s estabilizaÃ§Ã£o da rota testada | Medir atraso; Ã¡udio obrigatoriamente nos Ã³culos. |
| Foto nÃ£o chega | NÃ£o depender dela | Frame do stream | Stream ainda precisa ser validado com Ã¡udio. |
| Baixa qualidade | Uma recaptura orientada | Reposicionar bancada/cabeÃ§a e tentar novamente | Nunca converter falta de evidÃªncia em acerto. |
| TTS indisponÃ­vel | 5â€“10 min | Ãudios locais prÃ©-gerados | Rota dos alto-falantes continua indispensÃ¡vel. |
| Modelo/export corrompido | 10 min | Artefato anterior com hash conhecido | Revalidar compatibilidade e conjunto de casos. |
| Perda de conexÃ£o | Duas tentativas limitadas | Sample para diagnÃ³stico + revalidaÃ§Ã£o do checkpoint | Sem avanÃ§o/contagem extra de domÃ­nio. |
| Falha prÃ³xima ao pitch | Sem reestruturaÃ§Ã£o de Ãºltima hora | APK anterior e vÃ­deo real identificado | VÃ­deo nÃ£o deve ser apresentado como demonstraÃ§Ã£o ao vivo. |

### 15.2 Como preservar o checkpoint de IA em uma contingÃªncia visual

**Rota principal:** detector aprendido local + motor de regras + voz offline. A IA visual Ã© diretamente demonstrÃ¡vel.

**Rota alternativa proposta:** HSV/ArUco para evidÃªncia visual + reconhecimento de voz por modelo aprendido offline, funcionando de verdade nos Ã³culos. HÃ¡ IA funcional no pipeline, mas a interpretaÃ§Ã£o visual passa a ser determinÃ­stica. **Levar essa composiÃ§Ã£o Ã  mentoria e explicar o refinamento**; nÃ£o presumir equivalÃªncia de avaliaÃ§Ã£o nem apresentar HSV/ArUco como detector neural.

Se a voz tambÃ©m for substituÃ­da apenas por botÃ£o e a visÃ£o for inteiramente determinÃ­stica, essa composiÃ§Ã£o **nÃ£o atende por si sÃ³ ao checkpoint de IA real**. Manter um componente aprendido funcional, ou reconhecer a lacuna e buscar orientaÃ§Ã£o. NÃ£o adicionar um classificador irrelevante apenas para preencher o requisito.

Fallback em nuvem nÃ£o faz parte da rota principal deste plano. SÃ³ ativar se preparado antes: API funcional, rede, cota, consentimento/finalidade, polÃ­tica de retenÃ§Ã£o do provedor e documentaÃ§Ã£o atualizada. NÃ£o enviar frames automaticamente como reaÃ§Ã£o oculta a uma falha local.

### 15.3 Ordem de cortes se o prazo apertar

1. Cortar refinamento visual da UI e relatÃ³rios sofisticados.
2. Cortar Ã­ndice de autonomia e indicadores educacionais agregados.
3. Cortar suporte a mÃºltiplos aparelhos/versÃµes alÃ©m dos necessÃ¡rios ao evento.
4. Cortar tuning/quantizaÃ§Ã£o que nÃ£o resolvam um problema medido.
5. Usar entrada manual como contingÃªncia explÃ­cita, preservando cÃ¢mera, IA e Ã¡udio reais.
6. Apresentar recorte curto das seis etapas, mantendo o procedimento completo disponÃ­vel e explicando a duraÃ§Ã£o.

**Preservar:** IA real, entrada nos Ã³culos, saÃ­da nos Ã³culos, decisÃ£o conservadora, estado, privacidade, estratÃ©gia de energia e ao menos uma transiÃ§Ã£o real de fading. Reduzir o MVP entregue de seis para trÃªs etapas seria limitaÃ§Ã£o relevante e precisa ser assumida/discutida; nÃ£o Ã© a estratÃ©gia padrÃ£o de preparaÃ§Ã£o.

### 15.4 NÃ£o fazer na vÃ©spera

- Trocar para uma stack multiplataforma ou novo framework de UI.
- Atualizar automaticamente todas as bibliotecas.
- Treinar com outro kit e esperar que generalize sem teste.
- Introduzir geraÃ§Ã£o livre de decisÃ£o por LLM para esconder falta de regras.
- Considerar cÃ¢mera de celular equivalente a teste nos Ã³culos.
- Ocultar modo mock, histÃ³rico prÃ©-carregado ou saÃ­da pelo celular.
- Apagar histÃ³rico/dataset de outra pessoa para â€œlimpar o projetoâ€.

## 16. Checklist de saÃ­da e decisÃµes para mentoria

### 16.1 DefiniÃ§Ã£o de pronto antes da viagem

- [ ] APK instala no celular-alvo e abre offline.
- [ ] Kit real, seis etapas, trÃªs categorias de erro e mensagens estÃ£o alinhados.
- [ ] Detector principal executa no telefone; contingÃªncia visual estÃ¡ instalada.
- [ ] HÃ¡ IA funcional mesmo na composiÃ§Ã£o alternativa escolhida.
- [ ] Comandos em portuguÃªs e rejeiÃ§Ã£o de fala irrelevante foram testados.
- [ ] TTS/Ã¡udio de reserva funcionam; teste real nos Ray-Ban estÃ¡ pendente ou documentado.
- [ ] Adaptador DAT compila e roda com MDK.
- [ ] Fonte local, mock e real sÃ£o identificadas sem troca oculta.
- [ ] Acerto avanÃ§a uma vez; erro/inconclusivo nÃ£o avanÃ§am.
- [ ] Inconclusivo nÃ£o prejudica primeira tentativa nem fading.
- [ ] Estado e domÃ­nio sobrevivem a reinÃ­cio; reconexÃ£o exige revalidaÃ§Ã£o.
- [ ] TransiÃ§Ã£o L3â†’L2 pode ser demonstrada de forma audÃ­vel e transparente.
- [ ] Cinco ciclos completos prÃ©-evento passaram.
- [ ] Logs, fÃ³rmulas de mÃ©tricas e ficha de bateria estÃ£o preparados.
- [ ] Folha de privacidade descreve o cÃ³digo real e separa coleta de operaÃ§Ã£o.
- [ ] APKs, modelos, sample DAT, apresentaÃ§Ã£o e backups estÃ£o disponÃ­veis localmente.
- [ ] Integrantes sabem seus papÃ©is e a Ã¡rvore de diagnÃ³stico.

### 16.2 QuestÃµes concretas para levar Ã  mentoria

1. Qual celular serÃ¡ efetivamente utilizado, considerando a divergÃªncia edital/e-mail?
2. A configuraÃ§Ã£o de voz/stream selecionada Ã© adequada ao firmware disponÃ­vel?
3. Se necessÃ¡rio, a combinaÃ§Ã£o de visÃ£o determinÃ­stica e ASR aprendido preserva o checkpoint de IA e a proposta avaliada?
4. Os trÃªs erros escolhidos representam adequadamente a verificaÃ§Ã£o prometida no kit real?
5. Qual duraÃ§Ã£o oficial do pitch e como tornar o Ã¡udio dos Ã³culos audÃ­vel Ã  banca?
6. HÃ¡ restriÃ§Ã£o operacional para a coleta explÃ­cita dos 50â€“100 frames e gravaÃ§Ã£o da demo?

Esses pontos sÃ£o decisÃµes para execuÃ§Ã£o futura. A criaÃ§Ã£o deste plano nÃ£o envia mensagens Ã  coordenaÃ§Ã£o nem pressupÃµe respostas.

### 16.3 Primeiro bloco de execuÃ§Ã£o recomendado

ComeÃ§ar por **A01â€“A10**, distribuir as frentes e iniciar **B01â€“B08** e **B15â€“B20** em paralelo humano. Assim que o kit estiver definido, B inicia coleta E01â€“E09 enquanto C formaliza C06â€“C12. O primeiro objetivo de programaÃ§Ã£o Ã© uma etapa completa no celular com IA real e Ã¡udio, seguida pelo mesmo caminho via MDK.

NÃ£o comeÃ§ar pela apresentaÃ§Ã£o final, por um dashboard ou pela ampliaÃ§Ã£o de funcionalidades. A sequÃªncia protege a preparaÃ§Ã£o contra os riscos de hardware, dados e dependÃªncias que ainda nÃ£o foram resolvidos.

## 17. Fontes e autoridade das decisÃµes

### 17.1 Materiais internos considerados

- [Edital AI Glasses Brasil 2026](../AI%20Glasses%20Brasil/Edital%20AI%20Glasses%20Brasil%202026%20%281%29.pdf).
- [OrientaÃ§Ãµes finais gerais](../AI%20Glasses%20Brasil/orienta%C3%A7%C3%B5es%20finais%20gerais.txt).
- [OrientaÃ§Ãµes finais especÃ­ficas](../AI%20Glasses%20Brasil/orienta%C3%A7%C3%B5es%20finais%20especificas.txt).
- [DocumentaÃ§Ã£o tÃ©cnica da final](../AI%20Glasses%20Brasil/StepWise_Documentacao_Tecnica_Completa.docx).
- Documento-MÃ£e, dossiÃª de pesquisa, formulÃ¡rio da segunda fase, slides das fases 1/2 e arquitetura Mermaid.
- CÃ³digo em `android/`, `ml/`, `docs/`, README e configuraÃ§Ãµes da raiz.

ExigÃªncias do edital/e-mail geral sÃ£o compromissos do programa. O e-mail especÃ­fico oferece recomendaÃ§Ãµes. Este plano propÃµe escolhas de engenharia, que precisam de validaÃ§Ã£o local; quando houver conflito de logÃ­stica ou escopo, registrar e levar Ã  organizaÃ§Ã£o. NÃ£o interpretar relato de bug como especificaÃ§Ã£o garantida.

### 17.2 Fontes oficiais consultadas em 15/09/2026

Os links para `main/master` podem mudar. Na execuÃ§Ã£o de B06/B26, registrar a revisÃ£o usada e guardar a referÃªncia correspondente.

| ID | Fonte | Uso no plano |
|---|---|---|
| S1 | [DAT CameraAccess â€” catÃ¡logo de versÃµes](https://raw.githubusercontent.com/facebook/meta-wearables-dat-android/main/samples/CameraAccess/gradle/libs.versions.toml) | Baseline AGP/Kotlin/DAT. |
| S2 | [DAT CameraAccess â€” build Android](https://raw.githubusercontent.com/facebook/meta-wearables-dat-android/main/samples/CameraAccess/app/build.gradle.kts) | API mÃ­nima do exemplo, SDK e JDK. |
| S3 | [DAT CameraAccess â€” Gradle Wrapper](https://raw.githubusercontent.com/facebook/meta-wearables-dat-android/main/samples/CameraAccess/gradle/wrapper/gradle-wrapper.properties) | Gradle usado no exemplo. |
| S4 | [DAT â€” README oficial](https://raw.githubusercontent.com/facebook/meta-wearables-dat-android/main/README.md) e [changelog](https://github.com/facebook/meta-wearables-dat-android/blob/main/CHANGELOG.md) | Artefatos, origem de distribuiÃ§Ã£o, sessÃ£o/cÃ¢mera e opt-outs. |
| S5 | [Model Maker â€” guia de customizaÃ§Ã£o](https://developers.google.com/edge/mediapipe/solutions/customization/object_detector) | Estado de manutenÃ§Ã£o, treino e exportaÃ§Ã£o. |
| S6 | [Model Maker â€” dependÃªncias oficiais](https://raw.githubusercontent.com/google-ai-edge/mediapipe/master/mediapipe/model_maker/requirements.txt) e [pacote 0.2.1.4](https://pypi.org/project/mediapipe-model-maker/0.2.1.4/) | Isolamento do ambiente e resoluÃ§Ã£o das dependÃªncias legadas. |
| S7 | [MediaPipe Object Detector â€” Android](https://developers.google.com/edge/mediapipe/solutions/vision/object_detector/android) | Runtime e necessidade de modelo compatÃ­vel. |
| S8 | [MediaPipe â€” build do exemplo Android](https://raw.githubusercontent.com/google-ai-edge/mediapipe-samples/main/examples/object_detection/android/app/build.gradle) | VersÃ£o candidata de Tasks Vision. |
| S9 | [Vosk â€” modelos oficiais](https://alphacephei.com/vosk/models) | Modelo pt-BR, licenÃ§a e limitaÃ§Ãµes a testar. |
| S10 | [Vosk â€” Android](https://alphacephei.com/vosk/android) e [build do exemplo](https://raw.githubusercontent.com/alphacep/vosk-android-demo/master/app/build.gradle) | IntegraÃ§Ã£o offline e versÃµes Android/JNA. |
| S11 | [Android â€” SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer) | Disponibilidade de reconhecimento on-device. |
| S12 | [Android â€” Audio Manager](https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-manager) e [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord) | Roteamento e verificaÃ§Ã£o do dispositivo efetivo. |
| S13 | [Android â€” Room](https://developer.android.com/jetpack/androidx/releases/room) | PersistÃªncia e processador de cÃ³digo. |
| S14 | [KSP â€” release 2.2.21-2.0.5](https://github.com/google/ksp/releases/tag/2.2.21-2.0.5) | Candidato compatÃ­vel com a linha Kotlin escolhida. |
| S15 | [OpenCV â€” ArUco](https://docs.opencv.org/4.x/d5/dae/tutorial_aruco_detection.html) | ContingÃªncia de identificaÃ§Ã£o/posiÃ§Ã£o por marcador. |
| S16 | [TensorFlow â€” instalaÃ§Ã£o](https://www.tensorflow.org/install/pip) | SeparaÃ§Ã£o de ambientes e limitaÃ§Ãµes de GPU Windows. |
| S17 | [CVAT â€” introduÃ§Ã£o](https://docs.cvat.ai/docs/getting_started/) | Ferramenta de anotaÃ§Ã£o. |
| S18 | [Relato Android: HFP/cÃ¢mera, discussÃ£o 82](https://github.com/facebook/meta-wearables-dat-android/discussions/82), [discussÃ£o 130](https://github.com/facebook/meta-wearables-dat-android/discussions/130) e [relato iOS de capturePhoto](https://github.com/facebook/meta-wearables-dat-ios/issues/260) | Relatos primÃ¡rios de problemas, sem garantia de reproduÃ§Ã£o na StepWise. |
| S19 | [Meta â€” Mock Device Kit](https://wearables.developer.meta.com/docs/mock-device-kit/) | SimulaÃ§Ã£o de dispositivo, mÃ­dia e estados. |
| S20 | [LGPD â€” texto legal](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm) | ReferÃªncia para decisÃµes de tratamento de dados, sem concluir conformidade do produto. |

**Resultado esperado do plano:** o evento comeÃ§a com um sistema completo em ambiente de desenvolvimento e uma integraÃ§Ã£o DAT jÃ¡ exercitada. O trabalho presencial se concentra nos comportamentos que sÃ³ o par real de Ã³culos pode confirmar, com alternativas preparadas e evidÃªncia suficiente para apresentar a StepWise com clareza.
