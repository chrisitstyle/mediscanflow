import json
import logging

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
from messaging_contracts import AnalysisRequestedEvent
from processing_status import ProcessingStatus
from processor import AnalysisProcessor
from pydantic import ValidationError
from storage import create_minio_client

logger = logging.getLogger(__name__)


def configure_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    )


def handle_message(
    channel,
    method,
    body,
    processor: AnalysisProcessor,
) -> None:
    try:
        raw_event = json.loads(body.decode("utf-8"))

        requested_event = AnalysisRequestedEvent.model_validate(raw_event)

        processing_status, event = processor.process(requested_event)
        routing_key = routing_key_for(processing_status)

        publish_event(
            channel=channel,
            routing_key=routing_key,
            event=event,
        )

        logger.info(
            "Published %s event for analysisId=%s",
            event.event_type,
            event.payload.analysis_id,
        )

        channel.basic_ack(delivery_tag=method.delivery_tag)

    except json.JSONDecodeError as exception:
        logger.warning(
            "Invalid JSON message. Rejecting without requeue. error=%s",
            exception,
        )

        channel.basic_nack(
            delivery_tag=method.delivery_tag,
            requeue=False,
        )

    except ValidationError as exception:
        logger.warning(
            "Invalid AnalysisRequested contract. Rejecting without requeue. error=%s",
            exception,
        )

        channel.basic_nack(
            delivery_tag=method.delivery_tag,
            requeue=False,
        )

    except Exception as exception:
        logger.exception(
            "Failed to handle message. Requeuing. error=%s",
            exception,
        )

        channel.basic_nack(
            delivery_tag=method.delivery_tag,
            requeue=True,
        )


def routing_key_for(
    processing_status: ProcessingStatus,
) -> str:
    if processing_status == ProcessingStatus.COMPLETED:
        return ANALYSIS_COMPLETED_ROUTING_KEY

    if processing_status == ProcessingStatus.FAILED:
        return ANALYSIS_FAILED_ROUTING_KEY

    raise ValueError(f"Unsupported processing status: {processing_status}")


def main() -> None:
    configure_logging()

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

    logger.info("AI inference worker started.")
    logger.info("Waiting for AnalysisRequested events...")

    channel.start_consuming()


if __name__ == "__main__":
    main()
