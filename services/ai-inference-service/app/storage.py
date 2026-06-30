import os
import tempfile
from pathlib import Path

from config import MinioSettings
from minio import Minio


def create_minio_client(settings: MinioSettings) -> Minio:
    return Minio(
        endpoint=settings.endpoint,
        access_key=settings.access_key,
        secret_key=settings.secret_key,
        secure=settings.secure,
    )


def download_input_file(
    minio_client: Minio,
    settings: MinioSettings,
    object_key: str,
) -> str:
    suffix = Path(object_key).suffix or ".img"

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
        temp_file_path = temp_file.name

    minio_client.fget_object(
        bucket_name=settings.bucket,
        object_name=object_key,
        file_path=temp_file_path,
    )

    return temp_file_path


def delete_file_if_exists(file_path: str | None) -> None:
    if file_path and os.path.exists(file_path):
        os.remove(file_path)
