FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV YOLO_CONFIG_DIR=/tmp/ultralytics

RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0 \
    libgl1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements-lock.txt .

RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements-lock.txt

RUN useradd --create-home appuser \
    && mkdir -p /tmp/ultralytics \
    && chown -R appuser:appuser /app /tmp/ultralytics

COPY --chown=appuser:appuser app ./app

USER appuser

CMD ["python", "app/main.py"]