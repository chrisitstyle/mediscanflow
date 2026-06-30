import json

from config import (
    get_minio_settings,
    get_model_settings,
    get_rabbitmq_settings,
    should_simulate_inference_failure,
)
from events import build_completed_event, build_failed_event
from inference import load_yolo_model, run_yolo_inference
from messaging import (
    ANALYSIS_COMPLETED_ROUTING_KEY,
    ANALYSIS_FAILED_ROUTING_KEY,
    ANALYSIS_REQUESTED_QUEUE,
    configure_rabbitmq,
    create_rabbitmq_connection,
    publish_event,
)
from storage import create_minio_client, delete_file_if_exists, download_input_file


def handle_message(
    channel,
    method,
    properties,
    body,
    minio_client,
    minio_settings,
    model,
    model_settings,
) -> None:
    requested_event = json.loads(body.decode("utf-8"))
    payload = requested_event["payload"]

    analysis_id = payload["analysisId"]
    object_key = payload["objectKey"]

    input_file_path = None

    print(f"Received AnalysisRequested event for analysisId={analysis_id}")
    print(f"Downloading input file from MinIO: objectKey={object_key}")

    try:
        if should_simulate_inference_failure():
            raise RuntimeError("Simulated inference failure")

        input_file_path = download_input_file(
            minio_client=minio_client,
            settings=minio_settings,
            object_key=object_key,
        )

        print(f"Downloaded input file to: {input_file_path}")
        print(f"Running YOLO inference for analysisId={analysis_id}")

        detections = run_yolo_inference(
            model=model,
            image_path=input_file_path,
            settings=model_settings,
        )

        completed_event = build_completed_event(
            requested_event=requested_event,
            detections=detections,
        )

        publish_event(
            channel=channel,
            routing_key=ANALYSIS_COMPLETED_ROUTING_KEY,
            event=completed_event,
        )

        print(
            f"Published AnalysisCompleted event for analysisId={analysis_id}. "
            f"detections={len(detections)}"
        )

        channel.basic_ack(delivery_tag=method.delivery_tag)

    except Exception as exception:
        error_message = str(exception)

        failed_event = build_failed_event(
            requested_event=requested_event,
            error_message=error_message,
        )

        publish_event(
            channel=channel,
            routing_key=ANALYSIS_FAILED_ROUTING_KEY,
            event=failed_event,
        )

        print(
            f"Published AnalysisFailed event for analysisId={analysis_id}: "
            f"{error_message}"
        )

        channel.basic_ack(delivery_tag=method.delivery_tag)

    finally:
        delete_file_if_exists(input_file_path)

        if input_file_path:
            print(f"Deleted temporary input file: {input_file_path}")


def main() -> None:
    rabbitmq_settings = get_rabbitmq_settings()
    minio_settings = get_minio_settings()
    model_settings = get_model_settings()

    connection = create_rabbitmq_connection(rabbitmq_settings)
    channel = connection.channel()

    configure_rabbitmq(channel)

    minio_client = create_minio_client(minio_settings)
    model = load_yolo_model(model_settings)

    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(
        queue=ANALYSIS_REQUESTED_QUEUE,
        on_message_callback=lambda ch, method, properties, body: handle_message(
            channel=ch,
            method=method,
            properties=properties,
            body=body,
            minio_client=minio_client,
            minio_settings=minio_settings,
            model=model,
            model_settings=model_settings,
        ),
    )

    print("AI inference worker started. Waiting for AnalysisRequested events...")
    channel.start_consuming()


if __name__ == "__main__":
    main()
