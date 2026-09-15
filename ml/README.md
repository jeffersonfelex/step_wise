# StepWise · ml/

Pipeline offline para preparar o dataset e treinar/exportar o detector
visual customizado (peças da montagem de engrenagens) usado pelo app
Android via MediaPipe Tasks / LiteRT.

Não faz parte do caminho crítico do app: roda no computador de
desenvolvimento, gera um modelo `.tflite`, que é copiado para
`android/app/src/main/assets/models/` antes do build.

## Setup

    python -m venv .venv
    source .venv/bin/activate  # Windows: .venv\Scripts\activate
    pip install -r requirements.txt
    cp ../.env.example ../.env  # e preencha os valores necessários

## Scripts

- `train.py` — treina o detector a partir do dataset em `data/`.
- `export_litert.py` — exporta o modelo treinado para o formato LiteRT
  (.tflite) consumido pelo app Android.
