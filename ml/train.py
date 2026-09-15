"""
Treino do detector visual customizado (peças da montagem de engrenagens).

Uso:
    python train.py --data-dir ./data --output-dir ./models

TODO: substituir pelo pipeline real (ex.: MediaPipe Model Maker) assim que
o dataset anotado estiver disponível em DATASET_DIR (.env).
"""
import argparse
import os

from dotenv import load_dotenv

load_dotenv()


def main() -> None:
    parser = argparse.ArgumentParser(description="Treina o detector visual local do StepWise.")
    parser.add_argument("--data-dir", default=os.getenv("DATASET_DIR", "./data"))
    parser.add_argument("--output-dir", default=os.getenv("MEDIAPIPE_MODEL_OUTPUT_DIR", "./models"))
    args = parser.parse_args()

    os.makedirs(args.output_dir, exist_ok=True)

    # TODO: carregar dataset anotado (classes: eixo_principal, engrenagem_azul,
    # engrenagem_vermelha, trava_eixo, carcaca, parafuso_apertado).
    # TODO: treinar via MediaPipe Model Maker (object detector) ou equivalente.

    print(f"[train] dataset: {args.data_dir} -> modelo em: {args.output_dir}")


if __name__ == "__main__":
    main()
