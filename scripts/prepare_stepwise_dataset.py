"""Prepara manifesto e divisao train/val/test do dataset real StepWise.

O script nao copia imagens. Ele gera CSVs com caminhos relativos para que o
treino consiga ler a pasta original sem duplicar centenas de arquivos.

Uso:
    python scripts/prepare_stepwise_dataset.py --dataset-root "dataset stepwise"
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import random
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}

CLASS_FOLDERS = {
    "pecas_isoladas/P1_torneira": "P1_torneira",
    "pecas_isoladas/P2": "P2",
    "pecas_isoladas/P3": "P3",
    "pecas_isoladas/P4": "P4",
    "pecas_isoladas/P5": "P5",
    "pecas_isoladas/P6": "P6",
    "pecas_separadas_juntas": "pecas_separadas_juntas",
    "estados_montagem/S1_torneira": "S1_torneira",
    "estados_montagem/S2_torneira_P2": "S2_torneira_P2",
    "estados_montagem/S3_torneira_P2_P3": "S3_torneira_P2_P3",
    "estados_montagem/S4_torneira_P2_P3_P4": "S4_torneira_P2_P3_P4",
    "estados_montagem/S5_torneira_P2_P3_P4_P5": "S5_torneira_P2_P3_P4_P5",
    "estados_montagem/S6_final": "S6_final",
    "erros_montagem/E1_ordem_errada": "E1_ordem_errada",
    "erros_montagem/E2_peca_invertida": "E2_peca_invertida",
    "erros_montagem/E3_peca_ausente": "E3_peca_ausente",
    "erros_montagem/E4_peca_solta": "E4_peca_solta",
}


@dataclass(frozen=True)
class Sample:
    rel_path: str
    label: str
    group: str


def image_files(folder: Path) -> list[Path]:
    return sorted(
        p
        for p in folder.iterdir()
        if p.is_file() and p.suffix.lower() in IMAGE_EXTENSIONS
    )


def stable_digest(samples: list[Sample]) -> str:
    h = hashlib.sha256()
    for item in sorted(samples, key=lambda s: s.rel_path):
        h.update(item.rel_path.encode("utf-8"))
        h.update(b"\0")
        h.update(item.label.encode("utf-8"))
        h.update(b"\n")
    return h.hexdigest()


def split_class(samples: list[Sample], seed: int) -> dict[str, list[Sample]]:
    rng = random.Random(seed)
    shuffled = samples[:]
    rng.shuffle(shuffled)
    n = len(shuffled)
    if n == 0:
        return {"train": [], "val": [], "test": []}

    test_count = max(1, round(n * 0.15)) if n >= 10 else max(1, n // 5)
    val_count = max(1, round(n * 0.15)) if n >= 10 else max(1, n // 5)

    if n >= 3 and test_count + val_count >= n:
        test_count = 1
        val_count = 1
    elif n < 3:
        test_count = 0
        val_count = 0

    test = shuffled[:test_count]
    val = shuffled[test_count : test_count + val_count]
    train = shuffled[test_count + val_count :]
    return {"train": sorted(train, key=lambda s: s.rel_path), "val": sorted(val, key=lambda s: s.rel_path), "test": sorted(test, key=lambda s: s.rel_path)}


def write_csv(path: Path, rows: list[Sample]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["path", "label", "group"])
        writer.writeheader()
        for row in rows:
            writer.writerow({"path": row.rel_path, "label": row.label, "group": row.group})


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset-root", default="dataset stepwise")
    parser.add_argument("--output-dir", default="ml/data/stepwise_real")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    dataset_root = Path(args.dataset_root).resolve()
    imagens_root = dataset_root / "imagens"
    output_dir = Path(args.output_dir)
    if not imagens_root.is_dir():
        raise SystemExit(f"Pasta de imagens nao encontrada: {imagens_root}")

    all_samples: list[Sample] = []
    class_counts: dict[str, int] = {}
    missing: list[str] = []

    for rel_folder, label in CLASS_FOLDERS.items():
        folder = imagens_root / Path(rel_folder)
        if not folder.is_dir():
            missing.append(rel_folder)
            class_counts[label] = 0
            continue
        files = image_files(folder)
        class_counts[label] = len(files)
        for file in files:
            all_samples.append(
                Sample(
                    rel_path=file.resolve().relative_to(dataset_root).as_posix(),
                    label=label,
                    group=rel_folder.split("/")[0],
                )
            )

    splits = {"train": [], "val": [], "test": []}
    for label in CLASS_FOLDERS.values():
        label_samples = [s for s in all_samples if s.label == label]
        label_splits = split_class(label_samples, args.seed + sum(ord(c) for c in label))
        for split_name, rows in label_splits.items():
            splits[split_name].extend(rows)

    for split_name in splits:
        splits[split_name] = sorted(splits[split_name], key=lambda s: (s.label, s.rel_path))
        write_csv(output_dir / "splits" / f"{split_name}.csv", splits[split_name])
    write_csv(output_dir / "splits" / "all.csv", sorted(all_samples, key=lambda s: (s.label, s.rel_path)))

    manifest = {
        "name": "stepwise_torneira_real_v1",
        "created_at_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "dataset_root": dataset_root.as_posix(),
        "images_root": imagens_root.as_posix(),
        "task": "image_classification",
        "seed": args.seed,
        "total_images": len(all_samples),
        "classes": class_counts,
        "splits": {name: len(rows) for name, rows in splits.items()},
        "missing_folders": missing,
        "sha256_index": stable_digest(all_samples),
        "notes": [
            "Dataset real capturado antes do evento com o kit fisico de torneira/conexoes.",
            "A divisao atual e estratificada por classe, usando caminhos relativos e sem copiar imagens.",
            "Este primeiro ciclo usa classificacao da imagem inteira para acelerar a validacao no Android.",
        ],
    }
    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
