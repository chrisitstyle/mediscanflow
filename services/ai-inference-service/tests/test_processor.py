from types import SimpleNamespace

import processor as processor_module
from messaging_contracts import (
    AnalysisCompletedEvent,
    AnalysisCompletedPayload,
    AnalysisDetection,
    AnalysisFailedEvent,
    AnalysisFailedPayload,
    AnalysisRequestedEvent,
)
from processing_status import ProcessingStatus
from processor import AnalysisProcessor

ANALYSIS_ID = "4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24"
ATTEMPT_ID = "55555555-5555-4555-8555-555555555555"
PATIENT_ID = "9efdb5f0-733e-4f59-8a78-6240e43237c7"

REQUESTED_EVENT_ID = "11111111-1111-4111-8111-111111111111"
RESULT_EVENT_ID = "22222222-2222-4222-8222-222222222222"
CORRELATION_ID = "33333333-3333-4333-8333-333333333333"

INPUT_OBJECT_KEY = f"analyses/{ANALYSIS_ID}/brain-scan.jpg"
RESULT_OBJECT_KEY = f"analyses/{ANALYSIS_ID}/result.jpg"

INPUT_FILE_PATH = "tmp/input.jpg"
RESULT_FILE_PATH = "tmp/result.jpg"

MODEL_NAME = "yolo-brain-tumor-detector"
MODEL_VERSION = "yolov8n"


def test_process_downloads_input_runs_inference_uploads_result_and_returns_completed_event(
    monkeypatch,
) -> None:
    minio_client = object()
    minio_settings = object()
    model = object()
    model_settings = create_model_settings()

    requested_event = analysis_requested_event()

    raw_detections = [
        {
            "label": "tumor",
            "confidence": 0.92,
            "x": 10,
            "y": 20,
            "width": 100,
            "height": 80,
        }
    ]

    expected_detections = [
        AnalysisDetection.model_validate(detection) for detection in raw_detections
    ]

    completed_event = analysis_completed_event(detections=expected_detections)

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

        return raw_detections, RESULT_FILE_PATH

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
                kwargs["model_name"],
                kwargs["model_version"],
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
            MODEL_NAME,
            MODEL_VERSION,
            RESULT_OBJECT_KEY,
            expected_detections,
        ),
    ]

    assert cleanup_calls == [
        INPUT_FILE_PATH,
        RESULT_FILE_PATH,
    ]
    assert completed_event.payload.attempt_id == requested_event.payload.attempt_id


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
    assert failed_event.payload.attempt_id == requested_event.payload.attempt_id


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
    failed_event: AnalysisFailedEvent,
) -> None:
    def fake_build_failed_event(**kwargs):
        assert kwargs["model_name"] == MODEL_NAME
        assert kwargs["model_version"] == MODEL_VERSION
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
        model_settings=create_model_settings(),
    )


def create_model_settings() -> SimpleNamespace:
    return SimpleNamespace(
        name=MODEL_NAME,
        version=MODEL_VERSION,
    )


def analysis_requested_event() -> AnalysisRequestedEvent:
    return AnalysisRequestedEvent.model_validate(
        {
            "eventId": REQUESTED_EVENT_ID,
            "eventType": "AnalysisRequested",
            "eventVersion": 3,
            "occurredAt": "2026-07-04T10:00:00+00:00",
            "correlationId": CORRELATION_ID,
            "payload": {
                "analysisId": ANALYSIS_ID,
                "patientId": PATIENT_ID,
                "objectKey": INPUT_OBJECT_KEY,
                "attemptId": ATTEMPT_ID,
            },
        }
    )


def analysis_completed_event(
    detections: list[AnalysisDetection],
) -> AnalysisCompletedEvent:
    return AnalysisCompletedEvent(
        event_id=RESULT_EVENT_ID,
        event_type="AnalysisCompleted",
        event_version=3,
        occurred_at="2026-07-04T10:00:08+00:00",
        correlation_id=CORRELATION_ID,
        payload=AnalysisCompletedPayload(
            analysis_id=ANALYSIS_ID,
            attempt_id=ATTEMPT_ID,
            model_name=MODEL_NAME,
            model_version=MODEL_VERSION,
            result_object_key=RESULT_OBJECT_KEY,
            detections=detections,
        ),
    )


def analysis_failed_event(
    error_message: str,
) -> AnalysisFailedEvent:
    return AnalysisFailedEvent(
        event_id=RESULT_EVENT_ID,
        event_type="AnalysisFailed",
        event_version=3,
        occurred_at="2026-07-04T10:00:08+00:00",
        correlation_id=CORRELATION_ID,
        payload=AnalysisFailedPayload(
            analysis_id=ANALYSIS_ID,
            attempt_id=ATTEMPT_ID,
            model_name=MODEL_NAME,
            model_version=MODEL_VERSION,
            error_message=error_message,
        ),
    )
