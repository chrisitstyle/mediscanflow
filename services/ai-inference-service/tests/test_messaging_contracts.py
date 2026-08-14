import json
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker
from messaging_contracts import (
    AnalysisCompletedEvent,
    AnalysisCompletedPayload,
    AnalysisDetection,
    AnalysisFailedEvent,
    AnalysisFailedPayload,
    AnalysisProcessingStartedEvent,
    AnalysisProcessingStartedPayload,
    AnalysisRequestedEvent,
)

CONTRACTS_DIR = Path(__file__).resolve().parents[3] / "contracts" / "messaging"

ANALYSIS_ID = "4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24"
ATTEMPT_ID = "55555555-5555-4555-8555-555555555555"
PATIENT_ID = "9efdb5f0-733e-4f59-8a78-6240e43237c7"

REQUESTED_EVENT_ID = "11111111-1111-4111-8111-111111111111"
COMPLETED_EVENT_ID = "22222222-2222-4222-8222-222222222222"
FAILED_EVENT_ID = "44444444-4444-4444-8444-444444444444"
CORRELATION_ID = "33333333-3333-4333-8333-333333333333"

MODEL_NAME = "yolo-brain-tumor-detector"
MODEL_VERSION = "yolov8n"

INPUT_OBJECT_KEY = f"analyses/{ANALYSIS_ID}/brain-scan.jpg"
RESULT_OBJECT_KEY = f"analyses/{ANALYSIS_ID}/result.jpg"


def test_analysis_processing_started_event_matches_schema() -> None:
    event = AnalysisProcessingStartedEvent(
        event_id=COMPLETED_EVENT_ID,
        event_type="AnalysisProcessingStarted",
        event_version=1,
        occurred_at="2026-07-04T10:00:01+00:00",
        correlation_id=CORRELATION_ID,
        payload=AnalysisProcessingStartedPayload(
            analysis_id=ANALYSIS_ID,
            attempt_id=ATTEMPT_ID,
        ),
    )

    assert_matches_schema(
        event,
        "analysis-processing-started.schema.json",
    )


def test_analysis_requested_event_matches_schema() -> None:
    event = AnalysisRequestedEvent.model_validate(
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

    assert_matches_schema(
        event=event,
        schema_name="analysis-requested.schema.json",
    )


def test_analysis_completed_event_matches_schema() -> None:
    event = AnalysisCompletedEvent(
        event_id=COMPLETED_EVENT_ID,
        event_type="AnalysisCompleted",
        event_version=3,
        occurred_at="2026-07-04T10:01:00+00:00",
        correlation_id=CORRELATION_ID,
        payload=AnalysisCompletedPayload(
            analysis_id=ANALYSIS_ID,
            attempt_id=ATTEMPT_ID,
            model_name=MODEL_NAME,
            model_version=MODEL_VERSION,
            result_object_key=RESULT_OBJECT_KEY,
            detections=[
                AnalysisDetection(
                    label="glioma",
                    confidence=0.92,
                    x=10,
                    y=20,
                    width=100,
                    height=80,
                )
            ],
        ),
    )

    assert_matches_schema(
        event=event,
        schema_name="analysis-completed.schema.json",
    )


def test_analysis_failed_event_matches_schema() -> None:
    event = AnalysisFailedEvent(
        event_id=FAILED_EVENT_ID,
        event_type="AnalysisFailed",
        event_version=3,
        occurred_at="2026-07-04T10:01:00+00:00",
        correlation_id=CORRELATION_ID,
        payload=AnalysisFailedPayload(
            analysis_id=ANALYSIS_ID,
            attempt_id=ATTEMPT_ID,
            model_name=MODEL_NAME,
            model_version=MODEL_VERSION,
            error_message="Model inference failed",
        ),
    )

    assert_matches_schema(
        event=event,
        schema_name="analysis-failed.schema.json",
    )


def assert_matches_schema(
    event,
    schema_name: str,
) -> None:
    schema = load_schema(schema_name)

    Draft202012Validator.check_schema(schema)

    validator = Draft202012Validator(
        schema,
        format_checker=FormatChecker(),
    )

    event_json = event.model_dump(
        by_alias=True,
        mode="json",
    )

    errors = sorted(
        validator.iter_errors(event_json),
        key=lambda error: list(error.path),
    )

    assert not errors, "\n".join(error.message for error in errors)


def load_schema(schema_name: str) -> dict:
    schema_path = CONTRACTS_DIR / schema_name

    with schema_path.open(
        encoding="utf-8",
    ) as schema_file:
        return json.load(schema_file)
