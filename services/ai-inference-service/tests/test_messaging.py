import json
from unittest.mock import Mock, call

import pytest
from messaging import (
    ANALYSIS_COMPLETED_QUEUE,
    ANALYSIS_COMPLETED_ROUTING_KEY,
    ANALYSIS_EXCHANGE,
    ANALYSIS_FAILED_QUEUE,
    ANALYSIS_FAILED_ROUTING_KEY,
    ANALYSIS_REQUESTED_QUEUE,
    ANALYSIS_REQUESTED_ROUTING_KEY,
    configure_rabbitmq,
    publish_event,
)


def test_configure_rabbitmq_declares_exchange_queues_bindings_and_confirms() -> None:
    channel = Mock()

    configure_rabbitmq(channel)

    channel.exchange_declare.assert_called_once_with(
        exchange=ANALYSIS_EXCHANGE,
        exchange_type="direct",
        durable=True,
    )

    channel.queue_declare.assert_has_calls(
        [
            call(queue=ANALYSIS_REQUESTED_QUEUE, durable=True),
            call(queue=ANALYSIS_COMPLETED_QUEUE, durable=True),
            call(queue=ANALYSIS_FAILED_QUEUE, durable=True),
        ]
    )

    channel.queue_bind.assert_has_calls(
        [
            call(
                queue=ANALYSIS_REQUESTED_QUEUE,
                exchange=ANALYSIS_EXCHANGE,
                routing_key=ANALYSIS_REQUESTED_ROUTING_KEY,
            ),
            call(
                queue=ANALYSIS_COMPLETED_QUEUE,
                exchange=ANALYSIS_EXCHANGE,
                routing_key=ANALYSIS_COMPLETED_ROUTING_KEY,
            ),
            call(
                queue=ANALYSIS_FAILED_QUEUE,
                exchange=ANALYSIS_EXCHANGE,
                routing_key=ANALYSIS_FAILED_ROUTING_KEY,
            ),
        ]
    )

    channel.confirm_delivery.assert_called_once_with()


def test_publish_event_publishes_persistent_json_message_with_mandatory_flag() -> None:
    channel = Mock()
    channel.basic_publish.return_value = True

    event = analysis_completed_event()

    publish_event(
        channel=channel,
        routing_key=ANALYSIS_COMPLETED_ROUTING_KEY,
        event=event,
    )

    channel.basic_publish.assert_called_once()

    published_message = channel.basic_publish.call_args.kwargs

    assert published_message["exchange"] == ANALYSIS_EXCHANGE
    assert published_message["routing_key"] == ANALYSIS_COMPLETED_ROUTING_KEY
    assert json.loads(published_message["body"]) == event
    assert published_message["mandatory"] is True

    properties = published_message["properties"]

    assert properties.content_type == "application/json"
    assert properties.delivery_mode == 2


def test_publish_event_raises_when_publication_is_not_confirmed() -> None:
    channel = Mock()
    channel.basic_publish.return_value = False

    with pytest.raises(
        RuntimeError,
        match="RabbitMQ did not confirm event publication",
    ):
        publish_event(
            channel=channel,
            routing_key=ANALYSIS_COMPLETED_ROUTING_KEY,
            event=analysis_completed_event(),
        )


def analysis_completed_event() -> dict:
    return {
        "eventId": "event-123",
        "eventType": "AnalysisCompleted",
        "eventVersion": 2,
        "occurredAt": "2026-07-04T10:00:00+00:00",
        "correlationId": "correlation-123",
        "payload": {
            "analysisId": "analysis-123",
            "modelName": "yolo-brain-tumor-detector",
            "modelVersion": "yolov8n",
            "resultObjectKey": "analyses/analysis-123/result.jpg",
            "detections": [],
        },
    }
