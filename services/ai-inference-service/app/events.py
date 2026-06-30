import uuid
from datetime import datetime, timezone


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def build_completed_event(requested_event: dict, detections: list[dict]) -> dict:
    payload = requested_event["payload"]

    return {
        "eventId": str(uuid.uuid4()),
        "eventType": "AnalysisCompleted",
        "eventVersion": 1,
        "occurredAt": utc_now(),
        "correlationId": requested_event.get("correlationId"),
        "payload": {
            "analysisId": payload["analysisId"],
            "modelName": payload.get("modelName", "yolo-brain-tumor-detector"),
            "modelVersion": payload.get("modelVersion", "yolov8n"),
            "detections": detections,
        },
    }


def build_failed_event(requested_event: dict, error_message: str) -> dict:
    payload = requested_event["payload"]

    return {
        "eventId": str(uuid.uuid4()),
        "eventType": "AnalysisFailed",
        "eventVersion": 1,
        "occurredAt": utc_now(),
        "correlationId": requested_event.get("correlationId"),
        "payload": {
            "analysisId": payload["analysisId"],
            "modelName": payload.get("modelName", "yolo-brain-tumor-detector"),
            "modelVersion": payload.get("modelVersion", "yolov8n"),
            "errorMessage": error_message,
        },
    }
