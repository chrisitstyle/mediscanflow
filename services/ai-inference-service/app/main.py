import json
import os
import tempfile
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path

import pika
from minio import Minio

ANALYSIS_EXCHANGE = "mediscanflow.analysis"
ANALYSIS_REQUESTED_QUEUE = "analysis.requested"
ANALYSIS_COMPLETED_ROUTING_KEY = "analysis.completed"
ANALYSIS_FAILED_ROUTING_KEY = "analysis.failed"

MINIO_BUCKET = os.getenv("MINIO_BUCKET", "medical-scans")


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def create_minio_client() -> Minio:
    endpoint = os.getenv("MINIO_ENDPOINT", "localhost:9000")
    access_key = os.getenv("MINIO_ACCESS_KEY", "mediscanflow")
    secret_key = os.getenv("MINIO_SECRET_KEY", "mediscanflow123")
    secure = os.getenv("MINIO_SECURE", "false").lower() == "true"

    return Minio(
        endpoint=endpoint,
        access_key=access_key,
        secret_key=secret_key,
        secure=secure,
    )


def download_input_file(minio_client: Minio, object_key: str) -> str:
    suffix = Path(object_key).suffix or ".img"

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
        temp_file_path = temp_file.name

    minio_client.fget_object(
        bucket_name=MINIO_BUCKET,
        object_name=object_key,
        file_path=temp_file_path,
    )

    return temp_file_path


def build_completed_event(requested_event: dict) -> dict:
    payload = requested_event["payload"]

    return {
        "eventId": str(uuid.uuid4()),
        "eventType": "AnalysisCompleted",
        "eventVersion": 1,
        "occurredAt": utc_now(),
        "correlationId": requested_event.get("correlationId"),
        "payload": {
            "analysisId": payload["analysisId"],
            "modelName": payload.get("modelName", "yolo-brain-tumor-detector"),
            "modelVersion": payload.get("modelVersion", "yolov8n"),
            "detections": [
                {
                    "label": "suspected_tumor",
                    "confidence": 0.87,
                    "x": 120,
                    "y": 80,
                    "width": 90,
                    "height": 110,
                }
            ],
        },
    }


def build_failed_event(requested_event: dict, error_message: str) -> dict:
    payload = requested_event["payload"]

    return {
        "eventId": str(uuid.uuid4()),
        "eventType": "AnalysisFailed",
        "eventVersion": 1,
        "occurredAt": utc_now(),
        "correlationId": requested_event.get("correlationId"),
        "payload": {
            "analysisId": payload["analysisId"],
            "modelName": payload.get("modelName", "yolo-brain-tumor-detector"),
            "modelVersion": payload.get("modelVersion", "yolov8n"),
            "errorMessage": error_message,
        },
    }


def publish_event(channel, routing_key: str, event: dict) -> None:
    channel.basic_publish(
        exchange=ANALYSIS_EXCHANGE,
        routing_key=routing_key,
        body=json.dumps(event),
        properties=pika.BasicProperties(
            content_type="application/json",
            delivery_mode=2,
        ),
    )


def main() -> None:
    rabbitmq_host = os.getenv("RABBITMQ_HOST", "localhost")
    rabbitmq_port = int(os.getenv("RABBITMQ_PORT", "5672"))
    rabbitmq_user = os.getenv("RABBITMQ_USER", "mediscanflow")
    rabbitmq_password = os.getenv("RABBITMQ_PASSWORD", "mediscanflow")

    credentials = pika.PlainCredentials(rabbitmq_user, rabbitmq_password)

    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host=rabbitmq_host,
            port=rabbitmq_port,
            credentials=credentials,
        )
    )

    channel = connection.channel()
    minio_client = create_minio_client()

    channel.exchange_declare(
        exchange=ANALYSIS_EXCHANGE,
        exchange_type="direct",
        durable=True,
    )

    channel.queue_declare(
        queue=ANALYSIS_REQUESTED_QUEUE,
        durable=True,
    )

    def handle_message(ch, method, properties, body):
        requested_event = json.loads(body.decode("utf-8"))
        payload = requested_event["payload"]

        analysis_id = payload["analysisId"]
        object_key = payload["objectKey"]

        input_file_path = None

        print(f"Received AnalysisRequested event for analysisId={analysis_id}")
        print(f"Downloading input file from MinIO: objectKey={object_key}")

        try:
            should_fail = (
                os.getenv("SIMULATE_INFERENCE_FAILURE", "false").lower() == "true"
            )

            if should_fail:
                raise RuntimeError("Simulated inference failure")

            input_file_path = download_input_file(minio_client, object_key)

            print(f"Downloaded input file to: {input_file_path}")

            time.sleep(3)

            completed_event = build_completed_event(requested_event)

            publish_event(
                channel=ch,
                routing_key=ANALYSIS_COMPLETED_ROUTING_KEY,
                event=completed_event,
            )

            print(f"Published AnalysisCompleted event for analysisId={analysis_id}")

            ch.basic_ack(delivery_tag=method.delivery_tag)

        except Exception as exception:
            error_message = str(exception)
            failed_event = build_failed_event(requested_event, error_message)

            publish_event(
                channel=ch,
                routing_key=ANALYSIS_FAILED_ROUTING_KEY,
                event=failed_event,
            )

            print(
                f"Published AnalysisFailed event for analysisId={analysis_id}: "
                f"{error_message}"
            )

            ch.basic_ack(delivery_tag=method.delivery_tag)

        finally:
            if input_file_path and os.path.exists(input_file_path):
                os.remove(input_file_path)
                print(f"Deleted temporary input file: {input_file_path}")

    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(
        queue=ANALYSIS_REQUESTED_QUEUE,
        on_message_callback=handle_message,
    )

    print("AI inference worker started. Waiting for AnalysisRequested events...")
    channel.start_consuming()


if __name__ == "__main__":
    main()
