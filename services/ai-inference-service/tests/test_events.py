from datetime import datetime

from events import (
    build_completed_event,
    build_failed_event,
    build_processing_started_event,
)
from messaging_contracts import AnalysisDetection, AnalysisRequestedEvent

ANALYSIS_ID = "4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24"
ATTEMPT_ID = "55555555-5555-4555-8555-555555555555"
PATIENT_ID = "9efdb5f0-733e-4f59-8a78-6240e43237c7"

REQUESTED_EVENT_ID = "11111111-1111-4111-8111-111111111111"
CORRELATION_ID = "33333333-3333-4333-8333-333333333333"

MODEL_NAME = "yolo-brain-tumor-detector"
MODEL_VERSION = "yolov8n"

INPUT_OBJECT_KEY = f"analyses/{ANALYSIS_ID}/brain-scan.jpg"
RESULT_OBJECT_KEY = f"analyses/{ANALYSIS_ID}/result.jpg"


def test_build_processing_started_event_creates_analysis_processing_started_event() -> (
    None
):
    requested_event = analysis_requested_event()

    event = build_processing_started_event(requested_event)

    assert event.event_type == "AnalysisProcessingStarted"
    assert event.event_version == 1
    assert event.correlation_id == requested_event.correlation_id
    assert event.payload.analysis_id == requested_event.payload.analysis_id
    assert event.payload.attempt_id == requested_event.payload.attempt_id


def test_build_completed_event_creates_analysis_completed_event() -> None:
    requested_event = analysis_requested_event()

    detections = [
        AnalysisDetection(
            label="tumor",
            confidence=0.92,
            x=10,
            y=20,
            width=100,
            height=80,
        )
    ]

    completed_event = build_completed_event(
        requested_event=requested_event,
        model_name=MODEL_NAME,
        model_version=MODEL_VERSION,
        result_object_key=RESULT_OBJECT_KEY,
        detections=detections,
    )

    assert completed_event.event_id
    assert completed_event.event_type == "AnalysisCompleted"
    assert completed_event.event_version == 3
    assert completed_event.correlation_id == requested_event.correlation_id
    assert_datetime(completed_event.occurred_at)

    payload = completed_event.payload

    assert payload.analysis_id == requested_event.payload.analysis_id
    assert payload.model_name == MODEL_NAME
    assert payload.model_version == MODEL_VERSION
    assert payload.result_object_key == RESULT_OBJECT_KEY
    assert payload.detections == detections
    assert completed_event.payload.attempt_id == requested_event.payload.attempt_id


def test_build_failed_event_creates_analysis_failed_event() -> None:
    requested_event = analysis_requested_event()

    failed_event = build_failed_event(
        requested_event=requested_event,
        model_name=MODEL_NAME,
        model_version=MODEL_VERSION,
        error_message="Model failed",
    )

    assert failed_event.event_id
    assert failed_event.event_type == "AnalysisFailed"
    assert failed_event.event_version == 3
    assert failed_event.correlation_id == requested_event.correlation_id
    assert_datetime(failed_event.occurred_at)

    payload = failed_event.payload

    assert payload.analysis_id == requested_event.payload.analysis_id
    assert payload.model_name == MODEL_NAME
    assert payload.model_version == MODEL_VERSION
    assert payload.error_message == "Model failed"
    assert failed_event.payload.attempt_id == requested_event.payload.attempt_id


def analysis_requested_event() -> AnalysisRequestedEvent:
    return AnalysisRequestedEvent.model_validate(
        {
            "eventId": REQUESTED_EVENT_ID,
            "eventType": "AnalysisRequested",
            "eventVersion": 3,
            "occurredAt": "2026-07-04T10:00:00+00:00",
            "correlationId": CORRELATION_ID,
            "payload": {
                "analysisId": ANALYSIS_ID,
                "patientId": PATIENT_ID,
                "objectKey": INPUT_OBJECT_KEY,
                "attemptId": ATTEMPT_ID,
            },
        }
    )


def assert_datetime(value: datetime) -> None:
    assert isinstance(value, datetime)
    assert value.tzinfo is not None
