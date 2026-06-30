import os

from config import ModelSettings
from ultralytics import YOLO


def load_yolo_model(settings: ModelSettings) -> YOLO:
    if not os.path.exists(settings.path):
        raise FileNotFoundError(
            f"YOLO model not found at {settings.path}. "
            "Place your trained YOLOv8n weights in the models directory."
        )

    model = YOLO(settings.path)
    print(f"Loaded YOLO model from: {settings.path}")

    return model


def run_yolo_inference(
    model: YOLO,
    image_path: str,
    settings: ModelSettings,
) -> list[dict]:
    results = model.predict(
        source=image_path,
        conf=settings.confidence_threshold,
        verbose=False,
    )

    detections = []

    for result in results:
        class_names = result.names

        if result.boxes is None:
            continue

        for box in result.boxes:
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            confidence = float(box.conf[0])
            class_id = int(box.cls[0])
            label = str(class_names[class_id])

            detections.append(
                {
                    "label": label,
                    "confidence": confidence,
                    "x": float(x1),
                    "y": float(y1),
                    "width": float(x2 - x1),
                    "height": float(y2 - y1),
                }
            )

    return detections
