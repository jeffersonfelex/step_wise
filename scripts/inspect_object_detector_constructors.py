import inspect

from mediapipe_model_maker import object_detector

for name in [
    "HParams",
    "ModelOptions",
    "ObjectDetectorOptions",
    "QATHParams",
]:
    item = getattr(object_detector, name)
    print(f"{name}{inspect.signature(item)}")

print("SupportedModels:", list(object_detector.SupportedModels))
