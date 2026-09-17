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

- `augment.py` — multiplica um dataset pequeno de fotos reais em várias
  variações sintéticas (rotação, brilho, desfoque, ruído, compressão),
  para não depender só da quantidade de capturas físicas. Ver a seção
  "Poucas imagens" abaixo.
- `train.py` — treina o detector a partir do dataset em `data/`.
- `export_litert.py` — exporta o modelo treinado para o formato LiteRT
  (.tflite) consumido pelo app Android.

## Poucas imagens: como multiplicar o dataset

O detector visual customizado é o maior risco do projeto (ver
`checklist-hackathon.md` no Project) — o tempo para coletar fotos reais
simulando o ponto de vista dos óculos é curto. Ordem de prioridade
recomendada:

1. **Extrair frames de vídeo em vez de fotos avulsas.** Gravar 20-30s de
   vídeo (celular na altura da testa, lente ultra-wide, variando ângulo e
   distância) e extrair 1 frame a cada 0.3-0.5s multiplica a quantidade
   de imagens em minutos. Ex.: `ffmpeg -i video.mp4 -vf fps=2 data/raw/frame_%03d.jpg`
2. **Anotar as caixas delimitadoras** das fotos reais no formato CSV que
   `augment.py` espera (ver `data/annotations.csv.example`). Ferramentas
   como LabelImg, Roboflow ou CVAT ajudam a fazer isso rápido.
3. **Rodar `augment.py`** para multiplicar cada foto anotada em várias
   variações sintéticas (iluminação, desfoque, ruído, compressão — que
   simula a perda pelo Bluetooth):

       python augment.py --input-dir ./data/raw --annotations ./data/annotations.csv \
           --output-dir ./data/augmented --variants-per-image 8

4. **Treinar com `train.py`** usando o dataset combinado (`data/augmented/annotations_augmented.csv`
   já inclui as anotações originais + as aumentadas).
5. Se mesmo assim o modelo não ficar confiável a tempo, o app já tem um
   **plano B determinístico** (segmentação por cor / marcadores ArUco,
   ver `com.stepwise.vision.CompositeDetector` no app Android) que não
   depende de dataset nenhum — ele entra automaticamente quando a
   confiança do modelo treinado for baixa.
