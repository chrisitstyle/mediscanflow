from datetime import datetime

from events import build_completed_event, build_failed_event


def test_build_completed_event_creates_analysis_completed_event() -> None:
    requested_event = analysis_requested_event()
    detections = [
        {
            "label": "tumor",
            "confidence": 0.92,
            "x": 10,
            "y": 20,
            "width": 100,
            "height": 80,
        }
    ]

    completed_event = build_completed_event(
        requested_event=requested_event,
        result_object_key="analyses/analysis-123/result.jpg",
        detections=detections,
    )

    assert completed_event["eventId"]
    assert completed_event["eventType"] == "AnalysisCompleted"
    assert completed_event["eventVersion"] == 1
    assert completed_event["correlationId"] == "correlation-123"
    assert_iso_datetime(completed_event["occurredAt"])

    payload = completed_event["payload"]

    assert payload["analysisId"] == "analysis-123"
    assert payload["modelName"] == "yolo-brain-tumor-detector"
    assert payload["modelVersion"] == "yolov8n"
    assert payload["resultObjectKey"] == "analyses/analysis-123/result.jpg"
    assert payload["detections"] == detections


def test_build_completed_event_uses_default_model_values_when_missing() -> None:
    requested_event = {
        "correlationId": "correlation-123",
        "payload": {
            "analysisId": "analysis-123",
        },
    }

    completed_event = build_completed_event(
        requested_event=requested_event,
        result_object_key="analyses/analysis-123/result.jpg",
        detections=[],
    )

    payload = completed_event["payload"]

    assert payload["modelName"] == "yolo-brain-tumor-detector"
    assert payload["modelVersion"] == "yolov8n"


def test_build_failed_event_creates_analysis_failed_event() -> None:
    requested_event = analysis_requested_event()

    failed_event = build_failed_event(
        requested_event=requested_event,
        error_message="Model failed",
    )

    assert failed_event["eventId"]
    assert failed_event["eventType"] == "AnalysisFailed"
    assert failed_event["eventVersion"] == 1
    assert failed_event["correlationId"] == "correlation-123"
    assert_iso_datetime(failed_event["occurredAt"])

    payload = failed_event["payload"]

    assert payload["analysisId"] == "analysis-123"
    assert payload["modelName"] == "yolo-brain-tumor-detector"
    assert payload["modelVersion"] == "yolov8n"
    assert payload["errorMessage"] == "Model failed"


def test_build_failed_event_uses_default_model_values_when_missing() -> None:
    requested_event = {
        "correlationId": "correlation-123",
        "payload": {
            "analysisId": "analysis-123",
        },
    }

    failed_event = build_failed_event(
        requested_event=requested_event,
        error_message="Model failed",
    )

    payload = failed_event["payload"]

    assert payload["modelName"] == "yolo-brain-tumor-detector"
    assert payload["modelVersion"] == "yolov8n"


def analysis_requested_event() -> dict:
    return {
        "eventId": "event-123",
        "eventType": "AnalysisRequested",
        "eventVersion": 1,
        "occurredAt": "2026-07-04T10:00:00+00:00",
        "correlationId": "correlation-123",
        "payload": {
            "analysisId": "analysis-123",
            "patientId": "patient-123",
            "objectKey": "analyses/analysis-123/brain-scan.jpg",
            "modelName": "yolo-brain-tumor-detector",
            "modelVersion": "yolov8n",
        },
    }


def assert_iso_datetime(value: str) -> None:
    datetime.fromisoformat(value)