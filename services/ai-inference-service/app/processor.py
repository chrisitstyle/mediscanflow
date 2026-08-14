import logging

from events import build_completed_event, build_failed_event
from inference import run_yolo_inference
from messaging_contracts import (
    AnalysisDetection,
    AnalysisRequestedEvent,
    AnalysisResultEvent,
)
from processing_status import ProcessingStatus
from storage import delete_file_if_exists, download_input_file, upload_result_file

logger = logging.getLogger(__name__)


class AnalysisProcessor:
    def __init__(self, minio_client, minio_settings, model, model_settings):
        self.minio_client = minio_client
        self.minio_settings = minio_settings
        self.model = model
        self.model_settings = model_settings

    def process(
        self,
        requested_event: AnalysisRequestedEvent,
    ) -> tuple[ProcessingStatus, AnalysisResultEvent]:
        analysis_id = requested_event.payload.analysis_id
        object_key = requested_event.payload.object_key

        input_file_path = None
        result_file_path = None

        logger.info(
            "Received AnalysisRequested event for analysisId=%s",
            analysis_id,
        )

        try:
            logger.info(
                "Downloading input file from MinIO: objectKey=%s",
                object_key,
            )

            input_file_path = download_input_file(
                minio_client=self.minio_client,
                settings=self.minio_settings,
                object_key=object_key,
            )

            logger.info(
                "Downloaded input file to: %s",
                input_file_path,
            )

            logger.info(
                "Running YOLO inference for analysisId=%s",
                analysis_id,
            )

            raw_detections, result_file_path = run_yolo_inference(
                model=self.model,
                image_path=input_file_path,
                settings=self.model_settings,
            )

            detections = [
                AnalysisDetection.model_validate(detection)
                for detection in raw_detections
            ]

            result_object_key = f"analyses/{analysis_id}/result.jpg"

            upload_result_file(
                minio_client=self.minio_client,
                settings=self.minio_settings,
                object_key=result_object_key,
                file_path=result_file_path,
            )

            logger.info(
                "Uploaded result image to MinIO: objectKey=%s",
                result_object_key,
            )

            logger.info(
                "Analysis completed for analysisId=%s. detections=%s",
                analysis_id,
                len(detections),
            )

            return ProcessingStatus.COMPLETED, build_completed_event(
                requested_event=requested_event,
                model_name=self.model_settings.name,
                model_version=self.model_settings.version,
                result_object_key=result_object_key,
                detections=detections,
            )

        except Exception as exception:
            error_message = str(exception)

            logger.exception(
                "Analysis failed for analysisId=%s. error=%s",
                analysis_id,
                error_message,
            )

            return ProcessingStatus.FAILED, build_failed_event(
                requested_event=requested_event,
                model_name=self.model_settings.name,
                model_version=self.model_settings.version,
                error_message=error_message,
            )

        finally:
            delete_file_if_exists(input_file_path)
            delete_file_if_exists(result_file_path)

            if input_file_path:
                logger.info(
                    "Deleted temporary input file: %s",
                    input_file_path,
                )

            if result_file_path:
                logger.info(
                    "Deleted temporary result file: %s",
                    result_file_path,
                )
