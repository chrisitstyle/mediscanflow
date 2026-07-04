import pytest

from main import routing_key_for
from messaging import (
    ANALYSIS_COMPLETED_ROUTING_KEY,
    ANALYSIS_FAILED_ROUTING_KEY,
)
from processing_status import ProcessingStatus


def test_routing_key_for_completed_status() -> None:
    routing_key = routing_key_for(ProcessingStatus.COMPLETED)

    assert routing_key == ANALYSIS_COMPLETED_ROUTING_KEY


def test_routing_key_for_failed_status() -> None:
    routing_key = routing_key_for(ProcessingStatus.FAILED)

    assert routing_key == ANALYSIS_FAILED_ROUTING_KEY


def test_routing_key_for_unsupported_status() -> None:
    with pytest.raises(ValueError, match="Unsupported processing status"):
        routing_key_for("unknown")