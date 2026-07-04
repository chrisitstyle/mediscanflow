# Helper commands for testing and dependency locking

PYTHON ?= python

LOCK_VENV := .venv-lock

ifeq ($(OS),Windows_NT)
	LOCK_PYTHON := $(LOCK_VENV)/Scripts/python.exe
else
	LOCK_PYTHON := $(LOCK_VENV)/bin/python
endif

.PHONY: lock-deps test clean-lock-venv

# Rebuild requirements-lock.txt from a clean temporary virtual environment
lock-deps:
	$(PYTHON) -c "import shutil; shutil.rmtree('$(LOCK_VENV)', ignore_errors=True)"
	$(PYTHON) -m venv $(LOCK_VENV)
	$(LOCK_PYTHON) -m pip install --upgrade pip
	$(LOCK_PYTHON) -m pip install -r requirements.txt
	$(LOCK_PYTHON) -m pip freeze > requirements-lock.txt
	$(PYTHON) -c "import shutil; shutil.rmtree('$(LOCK_VENV)', ignore_errors=True)"

# Run the pytest test suite using the currently active Python environment
test:
	$(PYTHON) -m pytest

# Remove the temporary virtual environment used only for dependency locking
clean-lock-venv:
	$(PYTHON) -c "import shutil; shutil.rmtree('$(LOCK_VENV)', ignore_errors=True)"