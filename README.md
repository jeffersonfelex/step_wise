# StepWise

Tutor adaptativo para montagens mecânicas, guiado por áudio, para os óculos
de realidade aumentada **Ray-Ban Meta Gen 2**. Verifica cada etapa de uma
montagem por visão computacional local e reduz progressivamente o nível de
assistência conforme o aprendiz demonstra domínio (fading adaptativo).

## Por que existe

Em aulas práticas de mecânica/mecatrônica, manuais estáticos obrigam o
aprendiz a interromper a montagem para consultar o próximo passo. A
StepWise acompanha o estado real da tarefa e guia por voz, sem tela e sem
interromper as mãos ocupadas.

## Como funciona (resumo)

1. O aprendiz seleciona o procedimento (ex.: "Montagem de Engrenagens") e
   diz "verificar" quando termina uma etapa.
2. Os óculos capturam 1 frame sob demanda (Meta DAT v0.9); um Quality Gate
   garante que a imagem é utilizável.
3. Um detector visual local (MediaPipe / LiteRT) roda inteiramente no
   smartphone e retorna classes, bounding boxes e scores.
4. O motor procedural confronta as detecções com regras explícitas da
   etapa e decide CORRECT, KNOWN_ERROR ou INCONCLUSIVE — sem LLM no
   caminho crítico.
5. O feedback é falado por Android TextToSpeech, roteado por Bluetooth aos
   alto-falantes open-ear dos óculos.
6. Acertos consecutivos reduzem o nível de assistência (fading L4→L1).

Diagrama completo em [`docs/architecture-stepwise.mmd`](docs/architecture-stepwise.mmd).

## Estrutura do repositório

    step_wise/
    ├── android/      # App Android/Kotlin (core do produto)
    ├── ml/            # Treino/exportação (offline) do detector visual
    ├── docs/          # Diagrama de arquitetura e documentação
    ├── .env.example   # Modelo de variáveis de ambiente
    └── .gitignore

## Setup

### 1. Variáveis de ambiente

    cp .env.example .env
    # edite .env com os valores reais (chaves do Meta DAT, caminhos locais, etc.)

### 2. App Android (`android/`)

Requer Android Studio (Ladybug ou mais recente) e JDK 17.

    cd android
    cp local.properties.example local.properties
    # edite local.properties com o caminho do seu Android SDK

Abra a pasta `android/` no Android Studio e rode o módulo `app` em um
dispositivo com Android 8.0+ (minSdk 26), pareado com os Ray-Ban Meta e
com o Meta DAT configurado.

### 3. Pipeline de ML (`ml/`)

    cd ml
    python -m venv .venv
    source .venv/bin/activate  # Windows: .venv\Scripts\activate
    pip install -r requirements.txt

Veja [`ml/README.md`](ml/README.md) para os passos de treino e exportação
do modelo para LiteRT.

## Stack

- **App:** Android nativo, Kotlin, Room (persistência local), Android
  SpeechRecognizer (ASR), Android TextToSpeech (TTS).
- **Óculos:** Ray-Ban Meta Gen 2 via **Meta DAT v0.9** (captura de frame
  sob demanda, sem streaming contínuo).
- **Visão computacional:** detector customizado local via MediaPipe /
  LiteRT — sem nuvem no caminho crítico.
- **ML (offline):** Python, para preparar o dataset e exportar o modelo
  usado pelo app.

## Status

MVP em desenvolvimento (hackathon). Ver `docs/` e os slides do projeto no
Project do Claude para o contexto completo (problema, decisões técnicas e
trade-offs).
