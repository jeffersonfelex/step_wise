from mediapipe_model_maker import object_detector

names = [name for name in dir(object_detector) if not name.startswith("_")]
for name in names:
    print(name)
