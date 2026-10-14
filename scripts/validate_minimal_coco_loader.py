from mediapipe_model_maker import object_detector

train = object_detector.Dataset.from_coco_folder("ml/data/minimal_coco/train")
validation = object_detector.Dataset.from_coco_folder("ml/data/minimal_coco/validation")

print("train", train.size)
print("validation", validation.size)
