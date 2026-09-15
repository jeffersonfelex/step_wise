"""
Exporta o modelo treinado para o formato LiteRT (.tflite) consumido pelo
detector local no app Android (com.stepwise.vision.MediaPipeLiteRtDetector).

Uso:
    python export_litert.py --model-dir ./models --output-dir ./models/litert
"""
import argparse
import os

from dotenv import load_dotenv

load_dotenv()


def main() -> None:
    parser = argparse.ArgumentParser(description="Exporta o detector treinado para LiteRT.")
    parser.add_argument("--model-dir", default=os.getenv("MEDIAPIPE_MODEL_OUTPUT_DIR", "./models"))
    parser.add_argument("--output-dir", default=os.getenv("LITERT_EXPORT_DIR", "./models/litert"))
    args = parser.parse_args()

    os.makedirs(args.output_dir, exist_ok=True)

    # TODO: converter/exportar para .tflite e copiar para
    # android/app/src/main/assets/models/detector.tflite

    print(f"[export_litert] modelo: {args.model_dir} -> LiteRT em: {args.output_dir}")


if __name__ == "__main__":
    main()
