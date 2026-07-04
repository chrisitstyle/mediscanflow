from events import build_completed_event, build_failed_event
from inference import run_yolo_inference
from storage import delete_file_if_exists, download_input_file, upload_result_file


class AnalysisProcessor:
    def __init__(self, minio_client, minio_settings, model, model_settings):
        self.minio_client = minio_client
        self.minio_settings = minio_settings
        self.model = model
        self.model_settings = model_settings

    def process(self, requested_event: dict) -> tuple[str, dict]:
        payload = requested_event["payload"]
        analysis_id = payload["analysisId"]
        object_key = payload["objectKey"]

        input_file_path = None
        result_file_path = None

        try:
            input_file_path = download_input_file(
                minio_client=self.minio_client,
                settings=self.minio_settings,
                object_key=object_key,
            )

            detections, result_file_path = run_yolo_inference(
                model=self.model,
                image_path=input_file_path,
                settings=self.model_settings,
            )

            result_object_key = f"analyses/{analysis_id}/result.jpg"

            upload_result_file(
                minio_client=self.minio_client,
                settings=self.minio_settings,
                object_key=result_object_key,
                file_path=result_file_path,
            )

            return "completed", build_completed_event(
                requested_event=requested_event,
                result_object_key=result_object_key,
                detections=detections,
            )
        except Exception as exception:
            return "failed", build_failed_event(
                requested_event=requested_event,
                error_message=str(exception),
            )
        finally:
            delete_file_if_exists(input_file_path)
            delete_file_if_exists(result_file_path)