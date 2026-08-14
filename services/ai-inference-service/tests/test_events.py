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
        model_name="yolo-brain-tumor-detector",
        model_version="yolov8n",
        result_object_key="analyses/analysis-123/result.jpg",
        detections=detections,
    )

    assert completed_event["eventId"]
    assert completed_event["eventType"] == "AnalysisCompleted"
    assert completed_event["eventVersion"] == 2
    assert completed_event["correlationId"] == "correlation-123"
    assert_iso_datetime(completed_event["occurredAt"])

    payload = completed_event["payload"]

    assert payload["analysisId"] == "analysis-123"
    assert payload["modelName"] == "yolo-brain-tumor-detector"
    assert payload["modelVersion"] == "yolov8n"
    assert payload["resultObjectKey"] == "analyses/analysis-123/result.jpg"
    assert payload["detections"] == detections


def test_build_failed_event_creates_analysis_failed_event() -> None:
    requested_event = analysis_requested_event()

    failed_event = build_failed_event(
        requested_event=requested_event,
        model_name="yolo-brain-tumor-detector",
        model_version="yolov8n",
        error_message="Model failed",
    )

    assert failed_event["eventId"]
    assert failed_event["eventType"] == "AnalysisFailed"
    assert failed_event["eventVersion"] == 2
    assert failed_event["correlationId"] == "correlation-123"
    assert_iso_datetime(failed_event["occurredAt"])

    payload = failed_event["payload"]

    assert payload["analysisId"] == "analysis-123"
    assert payload["modelName"] == "yolo-brain-tumor-detector"
    assert payload["modelVersion"] == "yolov8n"
    assert payload["errorMessage"] == "Model failed"
    assert failed_event["payload"]["modelName"] == "yolo-brain-tumor-detector"
    assert failed_event["payload"]["modelVersion"] == "yolov8n"


def analysis_requested_event() -> dict:
    return {
        "eventId": "event-123",
        "eventType": "AnalysisRequested",
        "eventVersion": 2,
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
