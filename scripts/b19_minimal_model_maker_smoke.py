from pathlib import Path

from mediapipe_model_maker import object_detector


DATA_ROOT = Path("ml/data/minimal_coco")
OUT_DIR = Path("ml/models/b19_smoke")
OUT_DIR.mkdir(parents=True, exist_ok=True)

train_data = object_detector.Dataset.from_coco_folder(str(DATA_ROOT / "train"))
validation_data = object_detector.Dataset.from_coco_folder(str(DATA_ROOT / "validation"))

options = object_detector.ObjectDetectorOptions(
    supported_model=object_detector.SupportedModels.MOBILENET_V2,
    hparams=object_detector.HParams(
        epochs=1,
        batch_size=1,
        steps_per_epoch=1,
        export_dir=str(OUT_DIR),
        shuffle=False,
    ),
)

model = object_detector.ObjectDetector.create(
    train_data=train_data,
    validation_data=validation_data,
    options=options,
)
model.export_tflite(export_dir=str(OUT_DIR), tflite_filename="stepwise_b19_smoke.tflite")
print(OUT_DIR / "stepwise_b19_smoke.tflite")
