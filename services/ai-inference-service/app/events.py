from datetime import datetime, timezone
from uuid import uuid4

from messaging_contracts import (
    AnalysisCompletedEvent,
    AnalysisCompletedPayload,
    AnalysisDetection,
    AnalysisFailedEvent,
    AnalysisFailedPayload,
    AnalysisRequestedEvent,
)


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def build_completed_event(
    requested_event: AnalysisRequestedEvent,
    model_name: str,
    model_version: str,
    result_object_key: str,
    detections: list[AnalysisDetection],
) -> AnalysisCompletedEvent:
    return AnalysisCompletedEvent(
        event_id=uuid4(),
        event_type="AnalysisCompleted",
        event_version=3,
        occurred_at=utc_now(),
        correlation_id=requested_event.correlation_id,
        payload=AnalysisCompletedPayload(
            analysis_id=requested_event.payload.analysis_id,
            attempt_id=requested_event.payload.attempt_id,
            model_name=model_name,
            model_version=model_version,
            result_object_key=result_object_key,
            detections=detections,
        ),
    )


def build_failed_event(
    requested_event: AnalysisRequestedEvent,
    model_name: str,
    model_version: str,
    error_message: str,
) -> AnalysisFailedEvent:
    return AnalysisFailedEvent(
        event_id=uuid4(),
        event_type="AnalysisFailed",
        event_version=3,
        occurred_at=utc_now(),
        correlation_id=requested_event.correlation_id,
        payload=AnalysisFailedPayload(
            analysis_id=requested_event.payload.analysis_id,
            attempt_id=requested_event.payload.attempt_id,
            model_name=model_name,
            model_version=model_version,
            error_message=error_message,
        ),
    )
