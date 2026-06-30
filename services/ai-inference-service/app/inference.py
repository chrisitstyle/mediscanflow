import os
import tempfile

import cv2
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
) -> tuple[list[dict], str]:
    results = model.predict(
        source=image_path,
        conf=settings.confidence_threshold,
        verbose=False,
    )

    detections = []

    if not results:
        annotated_image_path = create_empty_annotated_image(image_path)
        return detections, annotated_image_path

    result = results[0]
    class_names = result.names

    if result.boxes is not None:
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

    annotated_image = result.plot()
    annotated_image_path = create_temp_result_image_path()

    cv2.imwrite(annotated_image_path, annotated_image)

    return detections, annotated_image_path


def create_empty_annotated_image(image_path: str) -> str:
    image = cv2.imread(image_path)

    if image is None:
        raise RuntimeError(f"Could not read image for annotation: {image_path}")

    annotated_image_path = create_temp_result_image_path()
    cv2.imwrite(annotated_image_path, image)

    return annotated_image_path


def create_temp_result_image_path() -> str:
    with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as temp_file:
        return temp_file.name
