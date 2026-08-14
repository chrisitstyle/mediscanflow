import uuid
from datetime import datetime, timezone


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def build_completed_event(
    requested_event: dict,
    model_name: str,
    model_version: str,
    result_object_key: str,
    detections: list[dict],
) -> dict:
    payload = requested_event["payload"]

    return {
        "eventId": str(uuid.uuid4()),
        "eventType": "AnalysisCompleted",
        "eventVersion": 2,
        "occurredAt": utc_now(),
        "correlationId": requested_event.get("correlationId"),
        "payload": {
            "analysisId": payload["analysisId"],
            "modelName": model_name,
            "modelVersion": model_version,
            "resultObjectKey": result_object_key,
            "detections": detections,
        },
    }


def build_failed_event(
    requested_event: dict,
    model_name: str,
    model_version: str,
    error_message: str,
) -> dict:
    payload = requested_event["payload"]

    return {
        "eventId": str(uuid.uuid4()),
        "eventType": "AnalysisFailed",
        "eventVersion": 2,
        "occurredAt": utc_now(),
        "correlationId": requested_event.get("correlationId"),
        "payload": {
            "analysisId": payload["analysisId"],
            "modelName": model_name,
            "modelVersion": model_version,
            "errorMessage": error_message,
        },
    }
