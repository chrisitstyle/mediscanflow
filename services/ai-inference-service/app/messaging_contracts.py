from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class MessagingContract(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        validate_by_alias=True,
        validate_by_name=True,
        extra="forbid",
    )


class AnalysisRequestedPayload(MessagingContract):
    analysis_id: UUID
    patient_id: UUID
    object_key: str


class AnalysisRequestedEvent(MessagingContract):
    event_id: UUID
    event_type: Literal["AnalysisRequested"]
    event_version: Literal[2]
    occurred_at: datetime
    correlation_id: UUID
    payload: AnalysisRequestedPayload


class AnalysisDetection(MessagingContract):
    label: str
    confidence: float
    x: float
    y: float
    width: float
    height: float


class AnalysisCompletedPayload(MessagingContract):
    analysis_id: UUID
    model_name: str
    model_version: str
    result_object_key: str
    detections: list[AnalysisDetection]


class AnalysisCompletedEvent(MessagingContract):
    event_id: UUID
    event_type: Literal["AnalysisCompleted"]
    event_version: Literal[2]
    occurred_at: datetime
    correlation_id: UUID
    payload: AnalysisCompletedPayload


class AnalysisFailedPayload(MessagingContract):
    analysis_id: UUID
    model_name: str
    model_version: str
    error_message: str


class AnalysisFailedEvent(MessagingContract):
    event_id: UUID
    event_type: Literal["AnalysisFailed"]
    event_version: Literal[2]
    occurred_at: datetime
    correlation_id: UUID
    payload: AnalysisFailedPayload


type AnalysisResultEvent = AnalysisCompletedEvent | AnalysisFailedEvent
