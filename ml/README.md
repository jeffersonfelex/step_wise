# StepWise · ml/

Pipeline offline da percepção física (modo hardware). Roda no computador de
desenvolvimento e gera o modelo `.tflite` consumido pelo app; não faz parte
do caminho crítico do Android. O modo software usa provedores pré-treinados
por adaptadores independentes e não depende deste diretório.

## Estado atual

O kit da final é a montagem de torneira com conexões (peças `P1`–`P6`,
estados `S1`–`S6`, erros `E1`–`E4`); ver
[dataset real](../docs/final-2026-09/dataset-real-stepwise.md). O caminho em uso
é um **classificador da imagem inteira** (MobileNetV2), não um detector de caixas.

O `stepwise_real_classifier_v1.tflite` empacotado em
`android/mode-hardware/src/main/assets/models/` foi treinado do zero e teve
**16% de acurácia no teste**: serve para validar o pipeline no aparelho, não
para decidir etapas. O motor só aceita `CORRECT`/`KNOWN_ERROR` com confiança
≥ 0,85, então na prática esse modelo leva a `INCONCLUSIVE`. Retreinar com os
padrões atuais do `train.py` (pesos ImageNet, sem espelhamento) é o próximo passo.

## Setup

    python -m venv .venv
    source .venv/bin/activate  # Windows: .venv\Scripts\activate
    pip install -r requirements.txt
    cp ../.env.example ../.env  # e preencha os valores necessários

`requirements-train.lock.txt` e `requirements-tools.lock.txt` registram as
versões exatas do ambiente validado na final (WSL/Ubuntu para treino, Python
3.10 no Windows para ferramentas); ver
[treino e exportação](../docs/final-2026-09/b17-b19-treino-exportacao.md).

## Fluxo do classificador (da raiz do repositório)

    # 1. Índice e divisão train/val/test (não copia imagens)
    python scripts/prepare_stepwise_dataset.py --dataset-root "dataset stepwise"

    # 2. Treino + exportação .tflite, labels.json e manifest.json
    python ml/train.py --dataset-root "dataset stepwise" \
        --index-dir ml/data/stepwise_real \
        --output-dir ml/models/stepwise_real_classifier_v2 --epochs 15

    # 3. Métricas por classe, matriz de confusão e visão procedural
    python ml/evaluate.py --dataset-root "dataset stepwise" \
        --index-dir ml/data/stepwise_real \
        --model ml/models/stepwise_real_classifier_v2/stepwise_real_classifier_v2.tflite \
        --output-dir ml/models/stepwise_real_classifier_v2/eval

Opções de `train.py`:

- `--weights imagenet` (padrão) congela a base pré-treinada e treina só a
  cabeça; `--weights none` reproduz o v1 (rede inteira do zero).
- `--allow-horizontal-flip` liga o espelhamento. Fica desligado por padrão
  porque espelhar pode transformar uma montagem correta em algo parecido com
  `E2_peca_invertida` sem trocar o rótulo.

O dataset bruto (`dataset stepwise/`) e os modelos gerados (`ml/models/`) não
são versionados. Para atualizar o app, copie o `.tflite` e o `labels.json`
gerados para `android/mode-hardware/src/main/assets/models/` com os nomes do
v1, ou ajuste `TfliteClassifierDetector`.

## Caminho de detecção (caixas) e poucas imagens

`augment.py` multiplica fotos anotadas com caixas (CSV `image,class_name,xmin,
ymin,xmax,ymax`, coordenadas normalizadas) em variações de iluminação, desfoque,
ruído e compressão. É útil se o projeto voltar a um detector de objetos
(`export_litert.py` continua como esqueleto dessa exportação):

    pip install -r requirements-augment.txt
    python ml/augment.py --input-dir ./ml/data/raw --annotations ./ml/data/annotations.csv \
        --output-dir ./ml/data/augmented --variants-per-image 8

Dicas para datasets pequenos:

1. **Extrair frames de vídeo em vez de fotos avulsas.** 20–30 s de vídeo na
   altura da testa, variando ângulo e distância, a 2 fps:
   `ffmpeg -i video.mp4 -vf fps=2 data/raw/frame_%03d.jpg`.
2. **Não espelhar** imagens de classes sensíveis a orientação (mesmo padrão do
   `augment.py` e do `train.py`).
3. Se o modelo não ficar confiável, o app mantém o plano B: sem confiança
   suficiente o resultado é `INCONCLUSIVE`, e a verificação pode cair para o
   avaliador por IA quando houver chave configurada.
