import inspect

from mediapipe_model_maker import object_detector

items = [
    object_detector.Dataset,
    object_detector.ObjectDetector,
    object_detector.ObjectDetectorOptions,
    object_detector.HParams,
    object_detector.ModelOptions,
]

for item in items:
    print(f"\n## {item.__name__}")
    for name in dir(item):
        if name.startswith("_"):
            continue
        value = getattr(item, name)
        if callable(value):
            try:
                sig = inspect.signature(value)
            except (TypeError, ValueError):
                sig = "(signature unavailable)"
            print(f"{name}{sig}")
