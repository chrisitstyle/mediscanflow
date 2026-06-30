import json
import os
import time
import uuid
from datetime import datetime, timezone

import pika

ANALYSIS_EXCHANGE = "mediscanflow.analysis"
ANALYSIS_REQUESTED_QUEUE = "analysis.requested"
ANALYSIS_COMPLETED_ROUTING_KEY = "analysis.completed"


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


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


def main() -> None:
    rabbitmq_host = os.getenv("RABBITMQ_HOST", "localhost")
    rabbitmq_port = int(os.getenv("RABBITMQ_PORT", "5672"))
    rabbitmq_user = os.getenv("RABBITMQ_USER", "mediscanflow")
    rabbitmq_password = os.getenv("RABBITMQ_PASSWORD", "mediscanflow")

    credentials = pika.PlainCredentials(rabbitmq_user, rabbitmq_password)

    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host=rabbitmq_host, port=rabbitmq_port, credentials=credentials
        )
    )

    channel = connection.channel()

    channel.exchange_declare(
        exchange=ANALYSIS_EXCHANGE, exchange_type="direct", durable=True
    )

    channel.queue_declare(queue=ANALYSIS_REQUESTED_QUEUE, durable=True)

    def handle_message(ch, method, properties, body):
        requested_event = json.loads(body.decode("utf-8"))

        analysis_id = requested_event["payload"]["analysisId"]
        print(f"Received AnalysisRequested event for analysisId={analysis_id}")

        time.sleep(3)

        completed_event = build_completed_event(requested_event)

        ch.basic_publish(
            exchange=ANALYSIS_EXCHANGE,
            routing_key=ANALYSIS_COMPLETED_ROUTING_KEY,
            body=json.dumps(completed_event),
            properties=pika.BasicProperties(
                content_type="application/json", delivery_mode=2
            ),
        )

        print(f"Published AnalysisCompleted event for analysisId={analysis_id}")

        ch.basic_ack(delivery_tag=method.delivery_tag)

    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(
        queue=ANALYSIS_REQUESTED_QUEUE, on_message_callback=handle_message
    )

    print("AI inference worker started. Waiting for AnalysisRequested events...")
    channel.start_consuming()


if __name__ == "__main__":
    main()
