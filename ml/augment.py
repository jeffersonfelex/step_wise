"""
Aumenta (via data augmentation) um dataset pequeno de fotos do kit de
montagem, gerando múltiplas variações sintéticas de cada foto real —
sem precisar de novas capturas físicas.

Contexto (ver checklist-hackathon.md no Project): o detector visual
customizado é o maior risco do projeto, e o tempo para coletar fotos
reais simulando o ponto de vista dos óculos é curto. Este script
multiplica o dataset real aplicando transformações que aproximam as
condições variáveis do evento: iluminação, leve tremor de mão,
desfoque de movimento e a perda de qualidade que a foto sofre ao
passar pelo Bluetooth.

Formato de entrada esperado (CSV, uma linha por objeto anotado):
    image,class_name,xmin,ymin,xmax,ymax
    frame_001.jpg,engrenagem_azul,0.32,0.41,0.58,0.70
    frame_001.jpg,eixo_principal,0.10,0.20,0.25,0.55

(coordenadas normalizadas 0..1 — mesmo esquema de BoundingBox usado em
com.stepwise.vision.DetectionResult no app Android; classes devem bater
com as usadas em android/app/src/main/assets/procedures/montagem_engrenagens.json)

Uso:
    pip install -r requirements.txt
    python augment.py --input-dir ./data/raw --annotations ./data/annotations.csv \
        --output-dir ./data/augmented --variants-per-image 8

Saída:
    ./data/augmented/<nome-original>_aug<N>.jpg
    ./data/augmented/annotations_augmented.csv (linhas originais + aumentadas)

IMPORTANTE: não aplica espelhamento horizontal por padrão. Alguns erros
conhecidos do procedimento dependem de orientação (ex.: "eixo
invertido"), e espelhar a imagem pode inverter esse sinal sem que o
rótulo mude. Só use --allow-horizontal-flip se tiver certeza de que
nenhuma classe/erro do seu procedimento é sensível a espelhamento.
"""
import argparse
import csv
import os
from collections import defaultdict

from dotenv import load_dotenv

load_dotenv()

try:
    import albumentations as A
    import cv2
except ImportError as exc:  # pragma: no cover
    raise SystemExit(
        "Faltam dependências. Rode: pip install -r requirements.txt\n"
        f"Detalhe: {exc}"
    )


def build_transform(allow_horizontal_flip: bool) -> "A.Compose":
    """
    Transformações pensadas para o cenário do evento: iluminação variável
    (mesa perto de janela x luz artificial), leve tremor de mão segurando
    celular/óculos, e a perda de qualidade que o frame sofre até chegar
    ao companion via Bluetooth.
    """
    transforms = [
        A.Rotate(limit=15, border_mode=cv2.BORDER_REPLICATE, p=0.7),
        A.ShiftScaleRotate(
            shift_limit=0.05, scale_limit=0.15, rotate_limit=0,
            border_mode=cv2.BORDER_REPLICATE, p=0.5,
        ),
        A.RandomBrightnessContrast(brightness_limit=0.3, contrast_limit=0.3, p=0.8),
        A.HueSaturationValue(hue_shift_limit=8, sat_shift_limit=20, val_shift_limit=15, p=0.5),
        A.OneOf(
            [
                A.MotionBlur(blur_limit=7, p=1.0),
                A.GaussianBlur(blur_limit=(3, 7), p=1.0),
            ],
            p=0.4,
        ),
        A.ISONoise(p=0.3),
        A.ImageCompression(quality_lower=35, quality_upper=75, p=0.6),  # simula perda via Bluetooth
        A.RandomShadow(p=0.2),
    ]
    if allow_horizontal_flip:
        transforms.append(A.HorizontalFlip(p=0.5))

    return A.Compose(
        transforms,
        bbox_params=A.BboxParams(format="albumentations", label_fields=["class_labels"], min_visibility=0.3),
    )


def load_annotations(csv_path: str) -> dict:
    """Agrupa anotações por imagem: {'frame_001.jpg': [(class_name, xmin, ymin, xmax, ymax), ...]}"""
    by_image = defaultdict(list)
    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            by_image[row["image"]].append(
                (
                    row["class_name"],
                    float(row["xmin"]),
                    float(row["ymin"]),
                    float(row["xmax"]),
                    float(row["ymax"]),
                )
            )
    return by_image


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, value))


def augment_dataset(
    input_dir: str,
    annotations_csv: str,
    output_dir: str,
    variants_per_image: int,
    allow_horizontal_flip: bool,
) -> None:
    os.makedirs(output_dir, exist_ok=True)
    transform = build_transform(allow_horizontal_flip)
    by_image = load_annotations(annotations_csv)

    out_rows = []
    total_generated = 0

    for image_name, boxes in by_image.items():
        image_path = os.path.join(input_dir, image_name)
        image = cv2.imread(image_path)
        if image is None:
            print(f"[augment] aviso: não consegui abrir {image_path}, pulando")
            continue

        # mantém as anotações da imagem original também na saída, para referência
        for class_name, xmin, ymin, xmax, ymax in boxes:
            out_rows.append([image_name, class_name, xmin, ymin, xmax, ymax])

        bboxes = [[clamp01(xmin), clamp01(ymin), clamp01(xmax), clamp01(ymax)] for _, xmin, ymin, xmax, ymax in boxes]
        class_labels = [c for c, *_ in boxes]

        base_name, ext = os.path.splitext(image_name)

        for i in range(variants_per_image):
            result = transform(image=image, bboxes=bboxes, class_labels=class_labels)
            if not result["bboxes"]:
                continue  # augmentação agressiva demais cortou todas as caixas para fora do frame

            aug_name = f"{base_name}_aug{i:02d}{ext}"
            cv2.imwrite(os.path.join(output_dir, aug_name), result["image"])

            for (xmin, ymin, xmax, ymax), class_name in zip(result["bboxes"], result["class_labels"]):
                out_rows.append([aug_name, class_name, xmin, ymin, xmax, ymax])
            total_generated += 1

    out_csv = os.path.join(output_dir, "annotations_augmented.csv")
    with open(out_csv, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["image", "class_name", "xmin", "ymin", "xmax", "ymax"])
        writer.writerows(out_rows)

    print(f"[augment] {len(by_image)} imagens originais -> {total_generated} variações geradas")
    print(f"[augment] anotações combinadas em: {out_csv}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Multiplica o dataset via data augmentation.")
    parser.add_argument("--input-dir", default=os.getenv("DATASET_DIR", "./data/raw"))
    parser.add_argument("--annotations", default="./data/annotations.csv")
    parser.add_argument("--output-dir", default="./data/augmented")
    parser.add_argument("--variants-per-image", type=int, default=8)
    parser.add_argument("--allow-horizontal-flip", action="store_true")
    args = parser.parse_args()

    augment_dataset(
        input_dir=args.input_dir,
        annotations_csv=args.annotations,
        output_dir=args.output_dir,
        variants_per_image=args.variants_per_image,
        allow_horizontal_flip=args.allow_horizontal_flip,
    )


if __name__ == "__main__":
    main()
