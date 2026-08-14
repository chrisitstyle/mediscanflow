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
from messaging_contracts import (
    AnalysisCompletedEvent,
    AnalysisCompletedPayload,
)

ANALYSIS_ID = "4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24"
EVENT_ID = "11111111-1111-4111-8111-111111111111"
CORRELATION_ID = "33333333-3333-4333-8333-333333333333"

MODEL_NAME = "yolo-brain-tumor-detector"
MODEL_VERSION = "yolov8n"

RESULT_OBJECT_KEY = f"analyses/{ANALYSIS_ID}/result.jpg"


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

    published_body = json.loads(published_message["body"].decode("utf-8"))

    assert published_body == event.model_dump(
        by_alias=True,
        mode="json",
    )

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


def analysis_completed_event() -> AnalysisCompletedEvent:
    return AnalysisCompletedEvent(
        event_id=EVENT_ID,
        event_type="AnalysisCompleted",
        event_version=2,
        occurred_at="2026-07-04T10:00:00+00:00",
        correlation_id=CORRELATION_ID,
        payload=AnalysisCompletedPayload(
            analysis_id=ANALYSIS_ID,
            model_name=MODEL_NAME,
            model_version=MODEL_VERSION,
            result_object_key=RESULT_OBJECT_KEY,
            detections=[],
        ),
    )
