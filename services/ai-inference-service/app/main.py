import json

from config import (
    get_minio_settings,
    get_model_settings,
    get_rabbitmq_settings,
)
from inference import load_yolo_model
from messaging import (
    ANALYSIS_COMPLETED_ROUTING_KEY,
    ANALYSIS_FAILED_ROUTING_KEY,
    ANALYSIS_REQUESTED_QUEUE,
    configure_rabbitmq,
    create_rabbitmq_connection,
    publish_event,
)
from processor import AnalysisProcessor
from storage import create_minio_client


def handle_message(
        channel,
        method,
        body,
        processor: AnalysisProcessor,
) -> None:
    try:
        requested_event = json.loads(body.decode("utf-8"))

        processing_status, event = processor.process(requested_event)

        routing_key = routing_key_for(processing_status)

        publish_event(
            channel=channel,
            routing_key=routing_key,
            event=event,
        )

        print(
            f"Published {event['eventType']} event for "
            f"analysisId={event['payload']['analysisId']}"
        )

        channel.basic_ack(delivery_tag=method.delivery_tag)

    except json.JSONDecodeError as exception:
        print(f"Invalid JSON message. Rejecting without requeue. error={exception}")

        channel.basic_nack(
            delivery_tag=method.delivery_tag,
            requeue=False,
        )

    except Exception as exception:
        print(f"Failed to handle message. Requeuing. error={exception}")

        channel.basic_nack(
            delivery_tag=method.delivery_tag,
            requeue=True,
        )


def routing_key_for(processing_status: str) -> str:
    if processing_status == "completed":
        return ANALYSIS_COMPLETED_ROUTING_KEY

    if processing_status == "failed":
        return ANALYSIS_FAILED_ROUTING_KEY

    raise ValueError(f"Unsupported processing status: {processing_status}")


def main() -> None:
    rabbitmq_settings = get_rabbitmq_settings()
    minio_settings = get_minio_settings()
    model_settings = get_model_settings()

    connection = create_rabbitmq_connection(rabbitmq_settings)
    channel = connection.channel()

    configure_rabbitmq(channel)

    minio_client = create_minio_client(minio_settings)
    model = load_yolo_model(model_settings)

    processor = AnalysisProcessor(
        minio_client=minio_client,
        minio_settings=minio_settings,
        model=model,
        model_settings=model_settings,
    )

    channel.basic_qos(prefetch_count=1)

    channel.basic_consume(
        queue=ANALYSIS_REQUESTED_QUEUE,
        on_message_callback=lambda ch, method, _properties, body: handle_message(
            channel=ch,
            method=method,
            body=body,
            processor=processor,
        ),
    )

    print("AI inference worker started.")
    print("Waiting for AnalysisRequested events...")

    channel.start_consuming()


if __name__ == "__main__":
    main()