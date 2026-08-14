import json
from types import SimpleNamespace
from unittest.mock import Mock

import main as main_module
from main import handle_message, routing_key_for
from messaging import (
    ANALYSIS_COMPLETED_ROUTING_KEY,
    ANALYSIS_FAILED_ROUTING_KEY,
)
from processing_status import ProcessingStatus


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


def test_handle_message_publishes_event_and_acknowledges_message(monkeypatch) -> None:
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

    monkeypatch.setattr(main_module, "publish_event", fake_publish_event)

    handle_message(
        channel=channel,
        method=method,
        body=json.dumps(requested_event).encode("utf-8"),
        processor=processor,
    )

    assert processor.received_event == requested_event

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


def test_handle_message_nacks_with_requeue_when_publish_fails(monkeypatch) -> None:
    channel = Mock()
    method = SimpleNamespace(delivery_tag="delivery-123")
    requested_event = analysis_requested_event()

    processor = FakeProcessor(
        status=ProcessingStatus.COMPLETED,
        event=analysis_completed_event(),
    )

    def fake_publish_event(**_kwargs):
        raise RuntimeError("RabbitMQ publish failed")

    monkeypatch.setattr(main_module, "publish_event", fake_publish_event)

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
    def __init__(self, status, event: dict) -> None:
        self.status = status
        self.event = event
        self.received_event = None

    def process(self, requested_event: dict):
        self.received_event = requested_event

        return self.status, self.event


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


def analysis_completed_event() -> dict:
    return {
        "eventId": "event-456",
        "eventType": "AnalysisCompleted",
        "eventVersion": 2,
        "occurredAt": "2026-07-04T10:01:00+00:00",
        "correlationId": "correlation-123",
        "payload": {
            "analysisId": "analysis-123",
            "modelName": "yolo-brain-tumor-detector",
            "modelVersion": "yolov8n",
            "resultObjectKey": "analyses/analysis-123/result.jpg",
            "detections": [],
        },
    }
