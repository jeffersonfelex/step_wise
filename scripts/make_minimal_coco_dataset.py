import json
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path("ml/data/minimal_coco")
CATEGORY = {"id": 1, "name": "peca_teste", "supercategory": "stepwise"}


def write_split(name: str, start_id: int) -> None:
    split_dir = ROOT / name
    image_dir = split_dir / "images"
    image_dir.mkdir(parents=True, exist_ok=True)

    images = []
    annotations = []
    for idx in range(2):
        image_id = start_id + idx
        file_name = f"{name}_{idx}.png"
        img = Image.new("RGB", (256, 256), (30, 30, 30))
        draw = ImageDraw.Draw(img)
        x0 = 64 + idx * 8
        y0 = 72 + idx * 6
        box = [x0, y0, 80, 70]
        draw.rectangle([x0, y0, x0 + box[2], y0 + box[3]], fill=(40, 120, 230))
        img.save(image_dir / file_name)

        images.append(
            {
                "id": image_id,
                "file_name": file_name,
                "width": 256,
                "height": 256,
            }
        )
        annotations.append(
            {
                "id": image_id,
                "image_id": image_id,
                "category_id": 1,
                "bbox": box,
                "area": box[2] * box[3],
                "iscrowd": 0,
            }
        )

    payload = {
        "info": {"description": "StepWise synthetic pipeline smoke dataset"},
        "licenses": [],
        "images": images,
        "annotations": annotations,
        "categories": [CATEGORY],
    }
    (split_dir / "labels.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")


def main() -> None:
    write_split("train", 1)
    write_split("validation", 101)
    print(ROOT)


if __name__ == "__main__":
    main()
