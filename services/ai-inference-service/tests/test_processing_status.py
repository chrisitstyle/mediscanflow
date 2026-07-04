from processing_status import ProcessingStatus


def test_processing_status_values() -> None:
    assert ProcessingStatus.COMPLETED.value == "completed"
    assert ProcessingStatus.FAILED.value == "failed"