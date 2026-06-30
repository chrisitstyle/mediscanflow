# MediScanFlow

MediScanFlow is a distributed medical image analysis platform built as a portfolio project.

It allows users to create patients, upload medical scan images, process them asynchronously with an AI inference worker, and retrieve analysis results with detected regions highlighted on an annotated image.

The project demonstrates a production-oriented backend architecture using Spring Boot, PostgreSQL, RabbitMQ, MinIO, Docker Compose, and a Python-based YOLO inference service.

---

## Features

- Patient management
- Medical scan upload
- Asynchronous image analysis workflow
- RabbitMQ-based communication between backend and AI worker
- MinIO object storage for original and processed images
- YOLOv8n-based brain tumor detection worker
- Analysis status tracking
- Detection results with bounding boxes and confidence scores
- Annotated result image generation
- Presigned URLs for image access
- Global API error handling
- File upload validation
- Fully containerized local development environment

---

## Tech Stack

### Backend

- Java 25
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Spring AMQP
- Flyway
- PostgreSQL
- MinIO Java SDK
- Gradle

### AI Worker

- Python
- Ultralytics YOLO
- OpenCV
- Pika
- MinIO Python SDK

### Infrastructure

- Docker
- Docker Compose
- PostgreSQL
- RabbitMQ Management
- MinIO

---

## Repository Structure

```text
mediscanflow/
├── docs/
│   └── architecture.md
├── infra/
│   └── docker-compose.yml
├── services/
│   ├── medical-platform-service/
│   │   ├── src/
│   │   ├── build.gradle
│   │   └── Dockerfile
│   └── ai-inference-service/
│       ├── app/
│       ├── models/
│       │   └── .gitkeep
│       ├── requirements.txt
│       └── Dockerfile
└── README.md
```

---

## Main Services

### `medical-platform-service`

Spring Boot backend responsible for:

- exposing REST API endpoints,
- managing patients and analyses,
- storing metadata in PostgreSQL,
- uploading files to MinIO,
- publishing analysis requests to RabbitMQ,
- consuming completed and failed analysis events,
- returning analysis results and presigned image URLs.

### `ai-inference-service`

Python worker responsible for:

- consuming analysis request events from RabbitMQ,
- downloading uploaded images from MinIO,
- running YOLOv8n inference,
- generating annotated result images,
- uploading result images back to MinIO,
- publishing completed or failed events.

---

## Local Development

### Prerequisites

Required:

- Docker
- Docker Compose
- Git

Optional for local development outside Docker:

- Java 25
- Python 3.12+
- Gradle

---

## Model File

The YOLO model weights are not committed to the repository.

Place the model file here:

```text
services/ai-inference-service/models/yolov8n-brain-tumor.pt
```

Expected model path inside the worker container:

```text
models/yolov8n-brain-tumor.pt
```

---

## Running the System

From the repository root:

```bash
docker compose -f infra/docker-compose.yml up -d --build
```

Check containers:

```bash
docker compose -f infra/docker-compose.yml ps
```

Check backend logs:

```bash
docker logs -f mediscanflow-medical-platform-service
```

Check worker logs:

```bash
docker logs -f mediscanflow-ai-inference-service
```

Stop the system:

```bash
docker compose -f infra/docker-compose.yml down
```

Stop and remove volumes:

```bash
docker compose -f infra/docker-compose.yml down -v
```

---

## Local URLs

| Service             | URL                                         |
| ------------------- | ------------------------------------------- |
| Backend API         | `http://localhost:8080/api`                 |
| Backend health      | `http://localhost:8080/api/actuator/health` |
| RabbitMQ Management | `http://localhost:15672`                    |
| MinIO Console       | `http://localhost:9001`                     |
| MinIO API           | `http://localhost:9000`                     |

Default RabbitMQ credentials:

```text
username: mediscanflow
password: mediscanflow
```

Default MinIO credentials:

```text
username: mediscanflow
password: mediscanflow123
```

---

## API Endpoints

### Patients

Create patient:

```http
POST /api/patients
```

Example request:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-01-15",
  "medicalRecordNumber": "MRN-0001"
}
```

List patients:

```http
GET /api/patients
```

Get patient by ID:

```http
GET /api/patients/{id}
```

---

### Analyses

Upload scan for patient:

```http
POST /api/patients/{patientId}/analyses
```

Request type:

```text
multipart/form-data
```

Required field:

```text
file
```

Optional parameters:

```text
modelName
modelVersion
```

Default model metadata:

```text
modelName: yolo-brain-tumor-detector
modelVersion: yolov8n
```

Get analysis by ID:

```http
GET /api/analyses/{id}
```

List analyses for patient:

```http
GET /api/patients/{patientId}/analyses
```

---

## Analysis Statuses

| Status       | Description                                 |
| ------------ | ------------------------------------------- |
| `UPLOADED`   | Scan was uploaded but not yet queued.       |
| `QUEUED`     | Analysis request was published to RabbitMQ. |
| `PROCESSING` | Reserved for future processing state.       |
| `COMPLETED`  | AI inference completed successfully.        |
| `FAILED`     | AI inference failed.                        |

Currently, uploaded analyses are moved directly to `QUEUED`, and then to either `COMPLETED` or `FAILED`.

---

## Example Analysis Response

```json
{
  "id": "4a5b2b52-f54c-4d2f-92fb-4b09c8341f7a",
  "patientId": "51e822a2-6c21-46cb-9f41-2209352c9b1a",
  "status": "COMPLETED",
  "originalFileName": "scan.jpg",
  "objectKey": "analyses/4a5b2b52-f54c-4d2f-92fb-4b09c8341f7a/scan.jpg",
  "resultObjectKey": "analyses/4a5b2b52-f54c-4d2f-92fb-4b09c8341f7a/result.jpg",
  "originalImageUrl": "http://localhost:9000/...",
  "resultImageUrl": "http://localhost:9000/...",
  "contentType": "image/jpeg",
  "fileSizeBytes": 123456,
  "modelName": "yolo-brain-tumor-detector",
  "modelVersion": "yolov8n",
  "errorMessage": null,
  "createdAt": "2026-06-30T16:20:00Z",
  "completedAt": "2026-06-30T16:20:10Z",
  "detections": [
    {
      "label": "Glioma",
      "confidence": 0.8913,
      "x": 120.5,
      "y": 80.0,
      "width": 64.2,
      "height": 58.7
    }
  ]
}
```

---

## Error Response Format

The backend returns a consistent error response format:

```json
{
  "timestamp": "2026-06-30T16:20:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Patient not found",
  "path": "/api/patients/00000000-0000-0000-0000-000000000000",
  "validationErrors": {}
}
```

Validation errors include field-level details:

```json
{
  "timestamp": "2026-06-30T16:20:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/patients",
  "validationErrors": {
    "firstName": "must not be blank",
    "lastName": "must not be blank"
  }
}
```

---

## File Upload Validation

Allowed content types:

```text
image/jpeg
image/png
```

Maximum upload size:

```text
10 MB
```

Invalid uploads return proper API errors:

| Case                     | Status                  |
| ------------------------ | ----------------------- |
| Missing file             | `400 Bad Request`       |
| Empty file               | `400 Bad Request`       |
| Unsupported content type | `400 Bad Request`       |
| File too large           | `413 Payload Too Large` |

---

## Event Flow

```text
Client
  |
  | POST /api/patients/{patientId}/analyses
  v
Spring Boot Backend
  |
  | Upload original image
  v
MinIO
  |
  | Publish AnalysisRequestedEvent
  v
RabbitMQ
  |
  | Consume event
  v
Python AI Worker
  |
  | Download image from MinIO
  | Run YOLO inference
  | Upload annotated image
  v
MinIO
  |
  | Publish AnalysisCompletedEvent or AnalysisFailedEvent
  v
RabbitMQ
  |
  | Consume result event
  v
Spring Boot Backend
  |
  | Update analysis status and detections
  v
PostgreSQL
```

---

## Configuration

The backend uses environment variables in Docker Compose for service-to-service communication:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mediscanflow
SPRING_RABBITMQ_HOST=rabbitmq
APP_STORAGE_MINIO_ENDPOINT=http://minio:9000
APP_STORAGE_MINIO_PUBLIC_ENDPOINT=http://localhost:9000
```

The AI worker uses:

```text
RABBITMQ_HOST=rabbitmq
MINIO_ENDPOINT=minio:9000
MODEL_PATH=models/yolov8n-brain-tumor.pt
YOLO_CONFIDENCE_THRESHOLD=0.25
```

---

## Development Notes

The project intentionally uses a simple modular structure instead of over-engineered domain layering.

Backend packages are organized by feature:

```text
patients/
analyses/
storage/
messaging/
common/
```

DTO classes use the `DTO` suffix.

RabbitMQ event contracts do not use the `DTO` suffix because they represent messaging contracts rather than REST API DTOs.

---

## Current Limitations

- No authentication or authorization yet.
- No frontend yet.
- No OpenAPI documentation yet.
- No Testcontainers integration tests yet.
- No production deployment manifests yet.
- AI model weights are handled manually and are not stored in Git.

---

## Planned Improvements

- Frontend web application
- Authentication and authorization
- OpenAPI documentation
- Integration tests with Testcontainers
- Transactional Outbox pattern
- Better AI model versioning
- DICOM support
- Cloud deployment configuration
