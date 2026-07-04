import processor as processor_module

from processing_status import ProcessingStatus
from processor import AnalysisProcessor

ANALYSIS_ID = "analysis-123"
INPUT_OBJECT_KEY = "analyses/analysis-123/brain-scan.jpg"
RESULT_OBJECT_KEY = "analyses/analysis-123/result.jpg"
INPUT_FILE_PATH = "tmp/input.jpg"
RESULT_FILE_PATH = "tmp/result.jpg"


def test_process_downloads_input_runs_inference_uploads_result_and_returns_completed_event(
        monkeypatch,
) -> None:
    minio_client = object()
    minio_settings = object()
    model = object()
    model_settings = object()

    requested_event = analysis_requested_event()
    detections = [
        {
            "label": "tumor",
            "confidence": 0.92,
            "x": 10,
            "y": 20,
            "width": 100,
            "height": 80,
        }
    ]
    completed_event = {
        "eventType": "AnalysisCompleted",
        "payload": {
            "analysisId": ANALYSIS_ID,
        },
    }

    calls = []

    def fake_download_input_file(**kwargs):
        calls.append(
            (
                "download",
                kwargs["minio_client"],
                kwargs["settings"],
                kwargs["object_key"],
            )
        )

        return INPUT_FILE_PATH

    def fake_run_yolo_inference(**kwargs):
        calls.append(
            (
                "inference",
                kwargs["model"],
                kwargs["image_path"],
                kwargs["settings"],
            )
        )

        return detections, RESULT_FILE_PATH

    def fake_upload_result_file(**kwargs):
        calls.append(
            (
                "upload",
                kwargs["minio_client"],
                kwargs["settings"],
                kwargs["object_key"],
                kwargs["file_path"],
            )
        )

    def fake_build_completed_event(**kwargs):
        calls.append(
            (
                "completed_event",
                kwargs["requested_event"],
                kwargs["result_object_key"],
                kwargs["detections"],
            )
        )

        return completed_event

    cleanup_calls = patch_cleanup(monkeypatch)

    monkeypatch.setattr(
        processor_module,
        "download_input_file",
        fake_download_input_file,
    )
    monkeypatch.setattr(
        processor_module,
        "run_yolo_inference",
        fake_run_yolo_inference,
    )
    monkeypatch.setattr(
        processor_module,
        "upload_result_file",
        fake_upload_result_file,
    )
    monkeypatch.setattr(
        processor_module,
        "build_completed_event",
        fake_build_completed_event,
    )

    processor = AnalysisProcessor(
        minio_client=minio_client,
        minio_settings=minio_settings,
        model=model,
        model_settings=model_settings,
    )

    status, event = processor.process(requested_event)

    assert status == ProcessingStatus.COMPLETED
    assert event == completed_event

    assert calls == [
        (
            "download",
            minio_client,
            minio_settings,
            INPUT_OBJECT_KEY,
        ),
        (
            "inference",
            model,
            INPUT_FILE_PATH,
            model_settings,
        ),
        (
            "upload",
            minio_client,
            minio_settings,
            RESULT_OBJECT_KEY,
            RESULT_FILE_PATH,
        ),
        (
            "completed_event",
            requested_event,
            RESULT_OBJECT_KEY,
            detections,
        ),
    ]

    assert cleanup_calls == [
        INPUT_FILE_PATH,
        RESULT_FILE_PATH,
    ]


def test_process_returns_failed_event_when_inference_fails_and_cleans_input_file(
        monkeypatch,
) -> None:
    requested_event = analysis_requested_event()
    failed_event = analysis_failed_event("YOLO failed")

    patch_download_input_file(monkeypatch)
    patch_failed_event_builder(
        monkeypatch=monkeypatch,
        expected_error_message="YOLO failed",
        failed_event=failed_event,
    )

    cleanup_calls = patch_cleanup(monkeypatch)

    def fake_run_yolo_inference(**_kwargs):
        raise RuntimeError("YOLO failed")

    def fake_upload_result_file(**_kwargs):
        raise AssertionError("Result file should not be uploaded")

    monkeypatch.setattr(
        processor_module,
        "run_yolo_inference",
        fake_run_yolo_inference,
    )
    monkeypatch.setattr(
        processor_module,
        "upload_result_file",
        fake_upload_result_file,
    )

    processor = create_processor()

    status, event = processor.process(requested_event)

    assert status == ProcessingStatus.FAILED
    assert event == failed_event

    assert cleanup_calls == [
        INPUT_FILE_PATH,
        None,
    ]


def test_process_returns_failed_event_when_upload_fails_and_cleans_temp_files(
        monkeypatch,
) -> None:
    requested_event = analysis_requested_event()
    failed_event = analysis_failed_event("MinIO upload failed")

    patch_download_input_file(monkeypatch)
    patch_failed_event_builder(
        monkeypatch=monkeypatch,
        expected_error_message="MinIO upload failed",
        failed_event=failed_event,
    )

    cleanup_calls = patch_cleanup(monkeypatch)

    def fake_run_yolo_inference(**_kwargs):
        return [], RESULT_FILE_PATH

    def fake_upload_result_file(**_kwargs):
        raise RuntimeError("MinIO upload failed")

    monkeypatch.setattr(
        processor_module,
        "run_yolo_inference",
        fake_run_yolo_inference,
    )
    monkeypatch.setattr(
        processor_module,
        "upload_result_file",
        fake_upload_result_file,
    )

    processor = create_processor()

    status, event = processor.process(requested_event)

    assert status == ProcessingStatus.FAILED
    assert event == failed_event

    assert cleanup_calls == [
        INPUT_FILE_PATH,
        RESULT_FILE_PATH,
    ]


def patch_download_input_file(monkeypatch) -> None:
    def fake_download_input_file(**_kwargs):
        return INPUT_FILE_PATH

    monkeypatch.setattr(
        processor_module,
        "download_input_file",
        fake_download_input_file,
    )


def patch_failed_event_builder(
        monkeypatch,
        expected_error_message: str,
        failed_event: dict,
) -> None:
    def fake_build_failed_event(**kwargs):
        assert kwargs["error_message"] == expected_error_message

        return failed_event

    monkeypatch.setattr(
        processor_module,
        "build_failed_event",
        fake_build_failed_event,
    )


def patch_cleanup(monkeypatch) -> list:
    cleanup_calls = []

    def fake_delete_file_if_exists(file_path):
        cleanup_calls.append(file_path)

    monkeypatch.setattr(
        processor_module,
        "delete_file_if_exists",
        fake_delete_file_if_exists,
    )

    return cleanup_calls


def create_processor() -> AnalysisProcessor:
    return AnalysisProcessor(
        minio_client=object(),
        minio_settings=object(),
        model=object(),
        model_settings=object(),
    )


def analysis_requested_event() -> dict:
    return {
        "eventId": "event-123",
        "eventType": "AnalysisRequested",
        "eventVersion": 1,
        "occurredAt": "2026-07-04T10:00:00+00:00",
        "correlationId": "correlation-123",
        "payload": {
            "analysisId": ANALYSIS_ID,
            "patientId": "patient-123",
            "objectKey": INPUT_OBJECT_KEY,
            "modelName": "yolo-brain-tumor-detector",
            "modelVersion": "yolov8n",
        },
    }


def analysis_failed_event(error_message: str) -> dict:
    return {
        "eventType": "AnalysisFailed",
        "payload": {
            "analysisId": ANALYSIS_ID,
            "errorMessage": error_message,
        },
    }