"""Avalia o classificador StepWise exportado em TFLite.

Gera métricas por classe, matriz de confusão e uma visão procedural reduzida.
Uso no WSL, a partir da raiz:

    python ml/evaluate.py \
      --dataset-root "dataset stepwise" \
      --index-dir ml/data/stepwise_real \
      --model ml/models/stepwise_real_classifier_v1/stepwise_real_classifier_v1.tflite \
      --output-dir ml/models/stepwise_real_classifier_v1/eval
"""

from __future__ import annotations

import argparse
import csv
import json
from collections import Counter, defaultdict
from pathlib import Path

import numpy as np
import tensorflow as tf
from PIL import Image


PROCEDURAL_LABELS = {
    "S1_torneira",
    "S2_torneira_P2",
    "S3_torneira_P2_P3",
    "S4_torneira_P2_P3_P4",
    "S5_torneira_P2_P3_P4_P5",
    "S6_final",
    "E1_ordem_errada",
    "E2_peca_invertida",
    "E3_peca_ausente",
    "E4_peca_solta",
}


def read_manifest(index_dir: Path) -> dict:
    return json.loads((index_dir / "manifest.json").read_text(encoding="utf-8"))


def load_rows(csv_path: Path, dataset_root: Path) -> list[dict[str, str]]:
    with csv_path.open(newline="", encoding="utf-8") as f:
        return [
            {
                "path": str(dataset_root / row["path"]),
                "rel_path": row["path"],
                "label": row["label"],
                "group": row["group"],
            }
            for row in csv.DictReader(f)
        ]


def preprocess(path: str, image_size: int) -> np.ndarray:
    image = Image.open(path).convert("RGB").resize((image_size, image_size))
    arr = np.asarray(image, dtype=np.float32)
    arr = (arr / 127.5) - 1.0
    return arr[None, ...]


def topk(scores: np.ndarray, labels: list[str], k: int = 3) -> list[dict[str, float | str]]:
    indices = np.argsort(scores)[::-1][:k]
    return [{"label": labels[int(i)], "score": float(scores[int(i)])} for i in indices]


def precision_recall_f1(tp: int, fp: int, fn: int) -> dict[str, float]:
    precision = tp / (tp + fp) if tp + fp else 0.0
    recall = tp / (tp + fn) if tp + fn else 0.0
    f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
    return {"precision": precision, "recall": recall, "f1": f1}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset-root", default="dataset stepwise")
    parser.add_argument("--index-dir", default="ml/data/stepwise_real")
    parser.add_argument("--split", default="test", choices=["train", "val", "test", "all"])
    parser.add_argument("--model", default="ml/models/stepwise_real_classifier_v1/stepwise_real_classifier_v1.tflite")
    parser.add_argument("--output-dir", default="ml/models/stepwise_real_classifier_v1/eval")
    parser.add_argument("--threshold", type=float, default=0.35)
    args = parser.parse_args()

    dataset_root = Path(args.dataset_root).resolve()
    index_dir = Path(args.index_dir).resolve()
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    manifest = read_manifest(index_dir)
    labels = list(manifest["classes"].keys())
    rows = load_rows(index_dir / "splits" / f"{args.split}.csv", dataset_root)

    interpreter = tf.lite.Interpreter(model_path=args.model)
    interpreter.allocate_tensors()
    input_info = interpreter.get_input_details()[0]
    output_info = interpreter.get_output_details()[0]
    image_size = int(input_info["shape"][1])

    predictions = []
    confusion: dict[str, Counter[str]] = defaultdict(Counter)
    procedural_confusion: dict[str, Counter[str]] = defaultdict(Counter)
    correct = 0
    top3_correct = 0
    accepted = 0
    accepted_correct = 0

    for row in rows:
        image = preprocess(row["path"], image_size).astype(input_info["dtype"])
        interpreter.set_tensor(input_info["index"], image)
        interpreter.invoke()
        scores = interpreter.get_tensor(output_info["index"])[0]
        ranked = topk(scores, labels, 3)
        pred = ranked[0]["label"]
        score = float(ranked[0]["score"])
        expected = row["label"]
        is_correct = pred == expected
        is_top3 = expected in {item["label"] for item in ranked}
        correct += int(is_correct)
        top3_correct += int(is_top3)
        confusion[expected][pred] += 1

        expected_proc = expected if expected in PROCEDURAL_LABELS else "NON_PROCEDURAL"
        pred_proc = pred if pred in PROCEDURAL_LABELS else "NON_PROCEDURAL"
        procedural_confusion[expected_proc][pred_proc] += 1

        if score >= args.threshold:
            accepted += 1
            accepted_correct += int(is_correct)

        predictions.append(
            {
                "path": row["rel_path"],
                "expected": expected,
                "predicted": pred,
                "score": score,
                "correct": is_correct,
                "top3_correct": is_top3,
                "top3": ranked,
            }
        )

    total = len(rows)
    per_class = {}
    for label in labels:
        tp = confusion[label][label]
        fp = sum(confusion[other][label] for other in labels if other != label)
        fn = sum(confusion[label][other] for other in labels if other != label)
        per_class[label] = {"support": sum(confusion[label].values()), **precision_recall_f1(tp, fp, fn)}

    procedural_labels = sorted(PROCEDURAL_LABELS | {"NON_PROCEDURAL"})
    per_procedural = {}
    for label in procedural_labels:
        tp = procedural_confusion[label][label]
        fp = sum(procedural_confusion[other][label] for other in procedural_labels if other != label)
        fn = sum(procedural_confusion[label][other] for other in procedural_labels if other != label)
        per_procedural[label] = {
            "support": sum(procedural_confusion[label].values()),
            **precision_recall_f1(tp, fp, fn),
        }

    result = {
        "model": args.model,
        "split": args.split,
        "total": total,
        "threshold": args.threshold,
        "accuracy": correct / total if total else 0.0,
        "top3_accuracy": top3_correct / total if total else 0.0,
        "accepted": accepted,
        "accepted_accuracy": accepted_correct / accepted if accepted else 0.0,
        "abstention_rate": 1 - (accepted / total) if total else 0.0,
        "per_class": per_class,
        "per_procedural": per_procedural,
        "confusion": {k: dict(v) for k, v in confusion.items()},
        "procedural_confusion": {k: dict(v) for k, v in procedural_confusion.items()},
    }

    (output_dir / f"{args.split}_metrics.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    (output_dir / f"{args.split}_predictions.jsonl").write_text(
        "\n".join(json.dumps(p, ensure_ascii=False) for p in predictions) + "\n",
        encoding="utf-8",
    )

    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
