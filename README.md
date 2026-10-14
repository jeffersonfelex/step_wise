# StepWise

Tutor por imagem e voz usando os óculos Ray-Ban Meta e um celular Android.
Dois modos evoluem de forma independente:

- **Hardware:** acompanhar a montagem de peças físicas (kit atual: torneira com
  conexões), verificando cada etapa por uma foto dos óculos.
- **Software:** interpretar um desenho técnico e orientar a modelagem CAD
  (Fusion 360 no tutor Gemini Live; Tinkercad no passo a passo narrado).

## Estado do código (outubro/2026)

Todo o trabalho das branches da final (18/09) foi integrado à base modular:

| Fluxo | Onde | Estado |
|---|---|---|
| Sessão de montagem com voz | `HardwareSessionActivity` + `mode-hardware` | Captura Meta DAT, classificador local, motor determinístico, checkpoints; IA Groq opcional quando o local não tem certeza. Compila e tem testes; falta validar com os óculos. |
| Guia manual de montagem | `HardwareTutorActivity` | Funciona sem óculos; confirmação manual das etapas. |
| Tutor CAD conversacional | `software-android/CadTutorActivity` | Gemini Live com token temporário, plano revisável, verificação por imagem. Testado com servidor simulado. |
| Passo a passo CAD narrado | `SoftwareSessionActivity` | Plano gerado pela Groq a partir do desenho, narrado por TTS e navegado por voz. |
| Privacidade | `PrivacyActivity` | Mostra e apaga o histórico de desempenho salvo no aparelho. |

O classificador visual v1 empacotado teve 16% de acurácia no teste e serve só para
validar o pipeline; ver [ml/README.md](ml/README.md). Registros da preparação da
final estão em [docs/final-2026-09/](docs/final-2026-09/).

## Onde cada equipe trabalha

| Diretório | Responsabilidade |
|---|---|
| [`android/mode-software/`](android/mode-software/README.md) | Tutor CAD, desenho técnico e integração com modelos (JVM puro) |
| [`android/mode-hardware/`](android/mode-hardware/README.md) | Motor físico, detectores, procedimentos, banco e testes |
| `android/software-android/` | Interface CAD, provedor Groq, importação de imagens e áudio em streaming |
| `android/core/` | Contratos comuns de objetivos, resultados, mídia e comandos de voz, sem Android |
| `android/platform-android/` | Óculos (Meta DAT), áudio/Vosk, cliente Groq e estado da conexão |
| `android/app/` | Composição dos módulos e telas companion |
| [`ml/`](ml/README.md) | Treino/exportação da percepção física |

Os modos não importam um ao outro. Leia o
[guia de desenvolvimento paralelo](docs/desenvolvimento-modular.md) para os
contratos e limites de dependência.

## Build

Requer JDK 17 (ou mais novo). Versões centralizadas em
[`android/gradle/libs.versions.toml`](android/gradle/libs.versions.toml):
Gradle 8.14.1, AGP 8.11.1, Kotlin 2.2.21, SDK 36, minSdk 31.

**Modo software sem Android SDK** (só JDK):

```bash
cd android
./gradlew -p jvm-tests :core:test :mode-software:test
```

**App Android** (JDK + Android SDK 36; configure `local.properties` a partir de
`local.properties.example`):

```bash
cd android
./gradlew assembleDebug testDebugUnitTest
```

O Meta DAT e o Vosk vêm do Maven Central (sem token). O primeiro build baixa o
modelo de voz Vosk (~32 MB, SHA-256 verificado) para
`platform-android/src/main/assets/model/`; também dá para usar
`scripts/download_vosk_model.sh`.

**Fronteiras entre módulos** (Python 3, da raiz):

```bash
python3 scripts/check_module_boundaries.py
```

O GitHub Actions ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) roda as
três verificações em cada push para `main`/`develop` e em pull requests.

## Credenciais

Nenhuma credencial fica no código ou no APK release:

- **Gemini Live:** token temporário gerado no computador com
  `scripts/create_gemini_live_token.py` e colado na tela do tutor.
- **Groq:** `groq.api.key` em `android/local.properties` (ou `GROQ_API_KEY`), lida
  apenas no build debug. Sem chave, o hardware verifica só localmente e o passo a
  passo narrado fica indisponível.

## Documentação

- [Desenvolvimento modular](docs/desenvolvimento-modular.md)
- [Primeiro fluxo do modo software](docs/modo-software-primeiro-fluxo.md) e seu fluxo de dados
- [Privacidade e dados do modo hardware (LGPD)](docs/lgpd-privacidade.md)
- [Arquitetura do modo hardware](docs/architecture-stepwise.mmd)
- [Pipeline de ML físico](ml/README.md)
- [Plano e registros da final de 18/09](docs/final-2026-09/PLANO_ACAO_FINAL_STEPWISE.md)
