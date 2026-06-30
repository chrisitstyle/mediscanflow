import json
import time

import pika
from config import RabbitMQSettings

ANALYSIS_EXCHANGE = "mediscanflow.analysis"

ANALYSIS_REQUESTED_QUEUE = "analysis.requested"
ANALYSIS_REQUESTED_ROUTING_KEY = "analysis.requested"

ANALYSIS_COMPLETED_QUEUE = "analysis.completed"
ANALYSIS_COMPLETED_ROUTING_KEY = "analysis.completed"

ANALYSIS_FAILED_QUEUE = "analysis.failed"
ANALYSIS_FAILED_ROUTING_KEY = "analysis.failed"


def create_rabbitmq_connection(settings: RabbitMQSettings):
    credentials = pika.PlainCredentials(settings.username, settings.password)

    connection_parameters = pika.ConnectionParameters(
        host=settings.host,
        port=settings.port,
        credentials=credentials,
        heartbeat=30,
        blocked_connection_timeout=30,
    )

    max_attempts = 30
    delay_seconds = 2

    for attempt in range(1, max_attempts + 1):
        try:
            print(
                f"Connecting to RabbitMQ at {settings.host}:{settings.port} "
                f"(attempt {attempt}/{max_attempts})..."
            )

            connection = pika.BlockingConnection(connection_parameters)

            print("Connected to RabbitMQ.")

            return connection

        except pika.exceptions.AMQPConnectionError as exception:
            print(
                f"RabbitMQ is not ready yet: {exception}. "
                f"Retrying in {delay_seconds} seconds..."
            )

            time.sleep(delay_seconds)

    raise RuntimeError("Could not connect to RabbitMQ after multiple attempts.")


def configure_rabbitmq(channel) -> None:
    channel.exchange_declare(
        exchange=ANALYSIS_EXCHANGE,
        exchange_type="direct",
        durable=True,
    )

    channel.queue_declare(
        queue=ANALYSIS_REQUESTED_QUEUE,
        durable=True,
    )
    channel.queue_bind(
        queue=ANALYSIS_REQUESTED_QUEUE,
        exchange=ANALYSIS_EXCHANGE,
        routing_key=ANALYSIS_REQUESTED_ROUTING_KEY,
    )

    channel.queue_declare(
        queue=ANALYSIS_COMPLETED_QUEUE,
        durable=True,
    )
    channel.queue_bind(
        queue=ANALYSIS_COMPLETED_QUEUE,
        exchange=ANALYSIS_EXCHANGE,
        routing_key=ANALYSIS_COMPLETED_ROUTING_KEY,
    )

    channel.queue_declare(
        queue=ANALYSIS_FAILED_QUEUE,
        durable=True,
    )
    channel.queue_bind(
        queue=ANALYSIS_FAILED_QUEUE,
        exchange=ANALYSIS_EXCHANGE,
        routing_key=ANALYSIS_FAILED_ROUTING_KEY,
    )


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
