import os
from dataclasses import dataclass


@dataclass(frozen=True)
class RabbitMQSettings:
    host: str
    port: int
    username: str
    password: str


@dataclass(frozen=True)
class MinioSettings:
    endpoint: str
    access_key: str
    secret_key: str
    bucket: str
    secure: bool


@dataclass(frozen=True)
class ModelSettings:
    path: str
    confidence_threshold: float


def get_rabbitmq_settings() -> RabbitMQSettings:
    return RabbitMQSettings(
        host=os.getenv("RABBITMQ_HOST", "localhost"),
        port=int(os.getenv("RABBITMQ_PORT", "5672")),
        username=os.getenv("RABBITMQ_USER", "mediscanflow"),
        password=os.getenv("RABBITMQ_PASSWORD", "mediscanflow"),
    )


def get_minio_settings() -> MinioSettings:
    return MinioSettings(
        endpoint=os.getenv("MINIO_ENDPOINT", "localhost:9000"),
        access_key=os.getenv("MINIO_ACCESS_KEY", "mediscanflow"),
        secret_key=os.getenv("MINIO_SECRET_KEY", "mediscanflow123"),
        bucket=os.getenv("MINIO_BUCKET", "medical-scans"),
        secure=os.getenv("MINIO_SECURE", "false").lower() == "true",
    )


def get_model_settings() -> ModelSettings:
    return ModelSettings(
        path=os.getenv("MODEL_PATH", "models/yolov8n-brain-tumor.pt"),
        confidence_threshold=float(os.getenv("YOLO_CONFIDENCE_THRESHOLD", "0.25")),
    )


def should_simulate_inference_failure() -> bool:
    return os.getenv("SIMULATE_INFERENCE_FAILURE", "false").lower() == "true"
