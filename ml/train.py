"""Treina e exporta o primeiro classificador visual real do StepWise.

Este pipeline usa a divisao gerada por `scripts/prepare_stepwise_dataset.py`.
Ele foi desenhado para o ciclo rapido da final: classificar a imagem inteira
em estados/casos do procedimento, exportar `.tflite` e validar cedo no POCO.

Uso a partir da raiz do repositorio, no ambiente Linux de treino:

    python ml/train.py \
      --dataset-root "dataset stepwise" \
      --index-dir ml/data/stepwise_real \
      --output-dir ml/models/stepwise_real_classifier_v1 \
      --epochs 3
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
import random
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
import tensorflow as tf


def read_manifest(index_dir: Path) -> dict:
    return json.loads((index_dir / "manifest.json").read_text(encoding="utf-8"))


def read_split(csv_path: Path, dataset_root: Path, label_to_id: dict[str, int]) -> tuple[list[str], list[int]]:
    paths: list[str] = []
    labels: list[int] = []
    with csv_path.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            paths.append(str(dataset_root / Path(row["path"])))
            labels.append(label_to_id[row["label"]])
    return paths, labels


def make_dataset(
    paths: list[str],
    labels: list[int],
    image_size: int,
    batch_size: int,
    training: bool,
) -> tf.data.Dataset:
    ds = tf.data.Dataset.from_tensor_slices((paths, labels))
    if training:
        ds = ds.shuffle(buffer_size=len(paths), seed=42, reshuffle_each_iteration=True)

    def load_image(path: tf.Tensor, label: tf.Tensor) -> tuple[tf.Tensor, tf.Tensor]:
        image = tf.io.read_file(path)
        image = tf.io.decode_image(image, channels=3, expand_animations=False)
        image = tf.image.resize(image, [image_size, image_size], antialias=True)
        image = tf.cast(image, tf.float32)
        image = tf.keras.applications.mobilenet_v2.preprocess_input(image)
        return image, label

    ds = ds.map(load_image, num_parallel_calls=tf.data.AUTOTUNE)
    return ds.batch(batch_size).prefetch(tf.data.AUTOTUNE)


def build_model(
    num_classes: int,
    image_size: int,
    learning_rate: float,
    weights: str | None = "imagenet",
    allow_horizontal_flip: bool = False,
) -> tf.keras.Model:
    inputs = tf.keras.Input(shape=(image_size, image_size, 3), name="image")
    x = inputs
    # Espelhar pode transformar uma montagem correta em "E2_peca_invertida" sem
    # trocar o rotulo (mesmo motivo documentado em augment.py): so com opt-in.
    if allow_horizontal_flip:
        x = tf.keras.layers.RandomFlip("horizontal", name="aug_flip")(x)
    x = tf.keras.layers.RandomRotation(0.04, name="aug_rotation")(x)
    x = tf.keras.layers.RandomZoom(0.08, name="aug_zoom")(x)
    base = tf.keras.applications.MobileNetV2(
        input_shape=(image_size, image_size, 3),
        include_top=False,
        weights=weights,
        alpha=0.35,
    )
    # Com pesos pre-treinados, congela a base: com ~400 imagens, treinar a rede
    # inteira do zero (weights=None, v1) ficou em 16% de acuracia no teste.
    base.trainable = weights is None
    x = base(x, training=weights is None)
    x = tf.keras.layers.GlobalAveragePooling2D(name="pool")(x)
    x = tf.keras.layers.Dropout(0.25, name="dropout")(x)
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax", name="class_scores")(x)
    model = tf.keras.Model(inputs, outputs, name="stepwise_real_classifier")
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset-root", default="dataset stepwise")
    parser.add_argument("--index-dir", default="ml/data/stepwise_real")
    parser.add_argument("--output-dir", default="ml/models/stepwise_real_classifier_v1")
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--batch-size", type=int, default=16)
    parser.add_argument("--image-size", type=int, default=224)
    parser.add_argument("--learning-rate", type=float, default=1e-3)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument(
        "--weights",
        choices=["imagenet", "none"],
        default="imagenet",
        help="Pesos iniciais do MobileNetV2 (imagenet = transfer learning, baixa os pesos na 1a vez).",
    )
    parser.add_argument(
        "--allow-horizontal-flip",
        action="store_true",
        help="Ativa espelhamento horizontal no augmentation (pode inverter o sinal de E2_peca_invertida).",
    )
    args = parser.parse_args()

    os.environ["PYTHONHASHSEED"] = str(args.seed)
    random.seed(args.seed)
    np.random.seed(args.seed)
    tf.random.set_seed(args.seed)

    dataset_root = Path(args.dataset_root).resolve()
    index_dir = Path(args.index_dir).resolve()
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    manifest = read_manifest(index_dir)
    labels = list(manifest["classes"].keys())
    label_to_id = {label: idx for idx, label in enumerate(labels)}

    train_paths, train_labels = read_split(index_dir / "splits" / "train.csv", dataset_root, label_to_id)
    val_paths, val_labels = read_split(index_dir / "splits" / "val.csv", dataset_root, label_to_id)
    test_paths, test_labels = read_split(index_dir / "splits" / "test.csv", dataset_root, label_to_id)

    train_ds = make_dataset(train_paths, train_labels, args.image_size, args.batch_size, training=True)
    val_ds = make_dataset(val_paths, val_labels, args.image_size, args.batch_size, training=False)
    test_ds = make_dataset(test_paths, test_labels, args.image_size, args.batch_size, training=False)

    model = build_model(
        len(labels),
        args.image_size,
        args.learning_rate,
        weights=None if args.weights == "none" else args.weights,
        allow_horizontal_flip=args.allow_horizontal_flip,
    )
    callbacks = [
        tf.keras.callbacks.ModelCheckpoint(
            filepath=str(output_dir / "checkpoint.keras"),
            monitor="val_accuracy",
            mode="max",
            save_best_only=True,
        )
    ]
    history = model.fit(train_ds, validation_data=val_ds, epochs=args.epochs, callbacks=callbacks)
    best_model = tf.keras.models.load_model(output_dir / "checkpoint.keras")
    test_loss, test_accuracy = best_model.evaluate(test_ds, verbose=1)

    saved_model_dir = output_dir / "saved_model"
    best_model.save(saved_model_dir, include_optimizer=False)

    converter = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_dir))
    tflite_model = converter.convert()
    tflite_path = output_dir / f"{output_dir.name}.tflite"
    tflite_path.write_bytes(tflite_model)

    (output_dir / "labels.json").write_text(
        json.dumps({"labels": labels}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    result = {
        "name": output_dir.name,
        "created_at_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "task": "image_classification",
        "dataset_manifest": str(index_dir / "manifest.json"),
        "dataset_sha256_index": manifest.get("sha256_index"),
        "labels": labels,
        "input": {
            "shape": [1, args.image_size, args.image_size, 3],
            "preprocess": "tf.keras.applications.mobilenet_v2.preprocess_input, RGB, resized square",
        },
        "splits": manifest.get("splits"),
        "epochs": args.epochs,
        "weights": args.weights,
        "allow_horizontal_flip": args.allow_horizontal_flip,
        "batch_size": args.batch_size,
        "history": history.history,
        "test": {"loss": float(test_loss), "accuracy": float(test_accuracy)},
        "tflite": {
            "path": str(tflite_path),
            "bytes": tflite_path.stat().st_size,
            "sha256": sha256_file(tflite_path),
        },
        "limits": [
            "Treino rapido com dataset pequeno e captura por celular.",
            "Modelo classifica a imagem inteira; nao prova encaixe mecanico quando a evidencia visual e insuficiente.",
            "Validacao no POCO com imagens reais e obrigatoria antes de integrar ao motor procedural.",
        ],
    }
    (output_dir / "manifest.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
