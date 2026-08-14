import json
from types import SimpleNamespace
from unittest.mock import Mock

import main as main_module
from main import handle_message, routing_key_for
from messaging import (
    ANALYSIS_COMPLETED_ROUTING_KEY,
    ANALYSIS_FAILED_ROUTING_KEY,
)
from messaging_contracts import (
    AnalysisCompletedEvent,
    AnalysisCompletedPayload,
    AnalysisRequestedEvent,
    AnalysisResultEvent,
)
from processing_status import ProcessingStatus

ANALYSIS_ID = "4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24"
PATIENT_ID = "9efdb5f0-733e-4f59-8a78-6240e43237c7"

REQUESTED_EVENT_ID = "11111111-1111-4111-8111-111111111111"
COMPLETED_EVENT_ID = "22222222-2222-4222-8222-222222222222"
CORRELATION_ID = "33333333-3333-4333-8333-333333333333"

MODEL_NAME = "yolo-brain-tumor-detector"
MODEL_VERSION = "yolov8n"

INPUT_OBJECT_KEY = f"analyses/{ANALYSIS_ID}/brain-scan.jpg"
RESULT_OBJECT_KEY = f"analyses/{ANALYSIS_ID}/result.jpg"


def test_routing_key_for_completed_status() -> None:
    routing_key = routing_key_for(ProcessingStatus.COMPLETED)

    assert routing_key == ANALYSIS_COMPLETED_ROUTING_KEY


def test_routing_key_for_failed_status() -> None:
    routing_key = routing_key_for(ProcessingStatus.FAILED)

    assert routing_key == ANALYSIS_FAILED_ROUTING_KEY


def test_routing_key_for_unsupported_status() -> None:
    try:
        routing_key_for("unknown")
    except ValueError as exception:
        assert "Unsupported processing status" in str(exception)
    else:
        raise AssertionError("Expected ValueError")


def test_handle_message_publishes_event_and_acknowledges_message(
    monkeypatch,
) -> None:
    channel = Mock()
    method = SimpleNamespace(delivery_tag="delivery-123")

    requested_event = analysis_requested_event()
    completed_event = analysis_completed_event()

    processor = FakeProcessor(
        status=ProcessingStatus.COMPLETED,
        event=completed_event,
    )

    publish_calls = []

    def fake_publish_event(**kwargs):
        publish_calls.append(kwargs)

    monkeypatch.setattr(
        main_module,
        "publish_event",
        fake_publish_event,
    )

    handle_message(
        channel=channel,
        method=method,
        body=json.dumps(requested_event).encode("utf-8"),
        processor=processor,
    )

    assert processor.received_event == AnalysisRequestedEvent.model_validate(
        requested_event
    )

    assert publish_calls == [
        {
            "channel": channel,
            "routing_key": ANALYSIS_COMPLETED_ROUTING_KEY,
            "event": completed_event,
        }
    ]

    channel.basic_ack.assert_called_once_with(delivery_tag="delivery-123")
    channel.basic_nack.assert_not_called()


def test_handle_message_rejects_invalid_json_without_requeue() -> None:
    channel = Mock()
    method = SimpleNamespace(delivery_tag="delivery-123")

    processor = FakeProcessor(
        status=ProcessingStatus.COMPLETED,
        event=analysis_completed_event(),
    )

    handle_message(
        channel=channel,
        method=method,
        body=b"{invalid-json",
        processor=processor,
    )

    assert processor.received_event is None

    channel.basic_nack.assert_called_once_with(
        delivery_tag="delivery-123",
        requeue=False,
    )
    channel.basic_ack.assert_not_called()


def test_handle_message_rejects_invalid_contract_without_requeue() -> None:
    channel = Mock()
    method = SimpleNamespace(delivery_tag="delivery-123")

    processor = FakeProcessor(
        status=ProcessingStatus.COMPLETED,
        event=analysis_completed_event(),
    )

    invalid_event = analysis_requested_event()
    invalid_event["eventVersion"] = 999

    handle_message(
        channel=channel,
        method=method,
        body=json.dumps(invalid_event).encode("utf-8"),
        processor=processor,
    )

    assert processor.received_event is None

    channel.basic_nack.assert_called_once_with(
        delivery_tag="delivery-123",
        requeue=False,
    )
    channel.basic_ack.assert_not_called()


def test_handle_message_nacks_with_requeue_when_publish_fails(
    monkeypatch,
) -> None:
    channel = Mock()
    method = SimpleNamespace(delivery_tag="delivery-123")

    requested_event = analysis_requested_event()

    processor = FakeProcessor(
        status=ProcessingStatus.COMPLETED,
        event=analysis_completed_event(),
    )

    def fake_publish_event(**_kwargs):
        raise RuntimeError("RabbitMQ publish failed")

    monkeypatch.setattr(
        main_module,
        "publish_event",
        fake_publish_event,
    )

    handle_message(
        channel=channel,
        method=method,
        body=json.dumps(requested_event).encode("utf-8"),
        processor=processor,
    )

    channel.basic_nack.assert_called_once_with(
        delivery_tag="delivery-123",
        requeue=True,
    )
    channel.basic_ack.assert_not_called()


def test_handle_message_nacks_with_requeue_when_processing_status_is_unsupported() -> (
    None
):
    channel = Mock()
    method = SimpleNamespace(delivery_tag="delivery-123")

    requested_event = analysis_requested_event()

    processor = FakeProcessor(
        status="unknown",
        event=analysis_completed_event(),
    )

    handle_message(
        channel=channel,
        method=method,
        body=json.dumps(requested_event).encode("utf-8"),
        processor=processor,
    )

    channel.basic_nack.assert_called_once_with(
        delivery_tag="delivery-123",
        requeue=True,
    )
    channel.basic_ack.assert_not_called()


class FakeProcessor:
    def __init__(
        self,
        status,
        event: AnalysisResultEvent,
    ) -> None:
        self.status = status
        self.event = event
        self.received_event: AnalysisRequestedEvent | None = None

    def process(
        self,
        requested_event: AnalysisRequestedEvent,
    ):
        self.received_event = requested_event

        return self.status, self.event


def analysis_requested_event() -> dict:
    return {
        "eventId": REQUESTED_EVENT_ID,
        "eventType": "AnalysisRequested",
        "eventVersion": 2,
        "occurredAt": "2026-07-04T10:00:00+00:00",
        "correlationId": CORRELATION_ID,
        "payload": {
            "analysisId": ANALYSIS_ID,
            "patientId": PATIENT_ID,
            "objectKey": INPUT_OBJECT_KEY,
        },
    }


def analysis_completed_event() -> AnalysisCompletedEvent:
    return AnalysisCompletedEvent(
        event_id=COMPLETED_EVENT_ID,
        event_type="AnalysisCompleted",
        event_version=2,
        occurred_at="2026-07-04T10:01:00+00:00",
        correlation_id=CORRELATION_ID,
        payload=AnalysisCompletedPayload(
            analysis_id=ANALYSIS_ID,
            model_name=MODEL_NAME,
            model_version=MODEL_VERSION,
            result_object_key=RESULT_OBJECT_KEY,
            detections=[],
        ),
    )
