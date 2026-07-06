# MediScanFlow

MediScanFlow is a distributed medical image analysis platform built as an advanced portfolio project.

It allows for comprehensive patient management, secure upload of medical scans, fully asynchronous processing using a dedicated AI inference worker (based on the YOLOv8 model), and convenient viewing of detection results directly in the web application, with pathological regions highlighted on the generated result image.

The project demonstrates a production-oriented approach to distributed system architecture, emphasizing security, reliable asynchronous communication, data consistency, and local development automation.

> **Disclaimer**: MediScanFlow is a demonstration and portfolio project. It is not a certified medical device and must not be used for real clinical diagnosis.

---

## 🌟 Key Features

- **Full Web Application:** a modern web interface for managing patients, uploading scans, reviewing analysis history, and viewing AI-generated results.

- **Authentication and Authorization:** centralized identity and access management using Keycloak with OAuth2/OIDC and Bearer JWT-secured API requests.

- **Role-Based Access Control:** separate permissions for `ADMIN`, `DOCTOR`, and `STAFF` users.

- **Patient Management:** creating, updating, archiving, restoring, and searching patient records.

- **Medical Scan Upload:** upload support for `JPEG` and `PNG` files with backend validation.

- **Asynchronous Processing Workflow:** separation of request handling from heavy AI inference using **RabbitMQ** messaging.

- **AI-Powered Detection:** python-based inference worker using YOLOv8n, Ultralytics, and OpenCV for medical image object detection.

- **S3-Compatible Object Storage:** storage of original scans and processed diagnostic images in `MinIO`, with temporary access through presigned URLs.

- **Analysis Results:** detection metadata including bounding boxes, confidence scores, and annotated result images.

- **PDF Reports:** downloadable PDF reports generated for completed analyses.

- **Dashboard:** overview of analysis statistics, recent activity, and platform state.

- **Audit Logging:** tracking of important domain and administrative actions.

- **Admin User Management:** user listing, creation, enabling, and disabling through the admin panel.

- **System Status View:** infrastructure component status available for administrators.

---

## 🏗 Tech Stack

### Backend - Platform Service

- Java 25
- Spring Boot 4.1.0
- Spring Web MVC & Spring Data JPA
- Spring AMQP (RabbitMQ)
- PostgreSQL
- Flyway
- MinIO Java SDK
- OpenPDF
- Gradle

### Web Application - Frontend

- TypeScript
- Next.js
- React
- TailwindCSS

### AI Worker - Inference Service

- Python 3.12+
- Ultralytics framework with YOLOv8 model
- Pika (RabbitMQ client) & MinIO Python SDK

### Infrastructure & Security

- Docker
- Keycloak

---

## 📂 Repository Structure

```text
mediscanflow/
├── infra/
│   ├── keycloak/
│   │   ├── mediscanflow-realm-dev.json # dev profile (default in d-compose)
│   │   └── mediscanflow-realm.json # prod profile (change )
│   └── docker-compose.yml
├──| services/
   ├── medical-platform-service/    # Backend - Spring Boot
   ├── ai-inference-service/        # AI Worker - Python + YOLO
   └── web-app/                     # Frontend - Next.js
```

---

## 🧠 Architecture Overview

```text
Browser
  |
  | Next.js web application
  v
Web App
  |
  | REST API requests with JWT access token
  v
Spring Boot Backend
  |
  +--> PostgreSQL
  |     stores patients, analyses, audit logs, and outbox events
  |
  +--> MinIO
  |     stores original and processed medical images
  |
  +--> RabbitMQ
        publishes analysis requests through the outbox publisher
        consumes completed or failed analysis events

RabbitMQ
  |
  | analysis.requested
  v
Python AI Worker
  |
  +--> MinIO
  |     downloads original image and uploads annotated result image
  |
  +--> RabbitMQ
        publishes analysis.completed or analysis.failed
```

## ⚙️ Services

The `web-app` service is a Next.js frontend responsible for:

- handling the Keycloak login and logout flow
- protecting application routes
- displaying the dashboard
- managing patient records
- uploading medical scans
- displaying analysis details and AI-generated results
- downloading generated PDF reports
- displaying audit activity
- providing an admin user-management interface
- showing system status information for administrators

### Medical Platform Service

The `medical-platform-service` is a Spring Boot backend responsible for:

- exposing REST API endpoints
- validating requests and uploaded files
- enforcing role-based authorization
- managing patients and analyses
- storing metadata in PostgreSQL
- storing medical images in MinIO
- publishing analysis requests through the Transactional Outbox pattern
- consuming completed and failed analysis events
- generating PDF reports
- recording audit events
- integrating with Keycloak Admin API for user management

### AI Inference Service

The `ai-inference-service` is a Python worker responsible for:

- consuming `analysis.requested` messages from RabbitMQ
- downloading scan images from MinIO
- running YOLO-based inference
- generating annotated result images
- uploading processed images back to MinIO
- publishing `analysis.completed` or `analysis.failed` events

## 🚀 Running the System - local development

The entire environment is fully containerized, and spinning it up requires just a few steps.

### Prerequisites

- Docker
- Git

### 1. Starting the Containers

From the main project directory (root), execute the command to build and start all containers in the background:

```bash
docker compose -f infra/docker-compose.yml up -d --build
```

### 2. Monitoring and Checking System Status

To verify the status of running services, view logs, or safely shut down the environment, use the following commands:

```bash
# check the status of containers and health checks
docker compose -f infra/docker-compose.yml ps

# follow web application logs
docker logs -f mediscanflow-web-app

# follow Spring Boot backend logs
docker logs -f mediscanflow-medical-platform-service

# follow AI worker logs
docker logs -f mediscanflow-ai-inference-service

# stop the entire system
docker compose -f infra/docker-compose.yml down

# stop the system and clean up data volumes (database, S3, etc.)
docker compose -f infra/docker-compose.yml down -v
```

---

## 🌍 Available Endpoints and Credentials

After successful startup, the services are available at the following local URLs:

|      Service / Application      |                     URL                     |           Development Credentials            |
| :-----------------------------: | :-----------------------------------------: | :------------------------------------------: |
|       **Web Application**       |           `http://localhost:3000`           |      Use one of the demo Keycloak users      |
|         **Backend API**         |         `http://localhost:8080/api`         | Requires `Authorization: Bearer <jwt_token>` |
|       **Backend Health**        | `http://localhost:8080/api/actuator/health` |                      -                       |
|   **Keycloak Admin Console**    |           `http://localhost:8081`           |              `admin` / `admin`               |
| **RabbitMQ Management Console** |          `http://localhost:15672`           |       `mediscanflow` / `mediscanflow`        |
|        **MinIO Console**        |           `http://localhost:9001`           |      `mediscanflow` / `mediscanflow123`      |
|     **MinIO API Endpoint**      |           `http://localhost:9000`           |  Used internally by S3-compatible services   |

---

## 🔐 Demo Users

The local Keycloak realm import contains demo users for testing different permission levels.

|  Role  |           Email           |   Password   |
| :----: | :-----------------------: | :----------: |
| Admin  | `admin@mediscanflow.com`  | `Admin123!`  |
| Doctor | `doctor@mediscanflow.com` | `Doctor123!` |
| Staff  | `staff@mediscanflow.com`  | `Staff123!`  |

> These credentials are intended for local development only.

## 🛡 Roles and Permissions

| Role     | Description                                                                |
| :------- | :------------------------------------------------------------------------- |
| `ADMIN`  | Full access, including user management and system status                   |
| `DOCTOR` | Medical data write access, including patient and analysis actions          |
| `STAFF`  | Read-only access to patients, analyses, dashboard, reports, and audit data |

High-level permission overview:

| Area                         | Admin | Doctor | Staff |
| :--------------------------- | :---: | :----: | :---: |
| Dashboard                    |  yes  |  yes   |  yes  |
| View patients                |  yes  |  yes   |  yes  |
| Create patients              |  yes  |  yes   |  no   |
| Update patients              |  yes  |  yes   |  no   |
| Archive and restore patients |  yes  |  yes   |  no   |
| Upload scans                 |  yes  |  yes   |  no   |
| View analyses                |  yes  |  yes   |  yes  |
| Retry failed analyses        |  yes  |  yes   |  no   |
| Download PDF reports         |  yes  |  yes   |  yes  |
| View audit logs              |  yes  |  yes   |  yes  |
| Manage users                 |  yes  |   no   |  no   |
| View system status           |  yes  |   no   |  no   |

## 📊 Analysis Lifecycle Model

The analysis workflow is represented using status values that make it possible to track progress from the web application.

| Status       | Description                                                                                                     |
| :----------- | :-------------------------------------------------------------------------------------------------------------- |
| `UPLOADED`   | The medical image has been uploaded and saved; the analysis request is waiting to be published from the outbox  |
| `QUEUED`     | The analysis request has been published to RabbitMQ and is waiting to be processed by an AI worker              |
| `PROCESSING` | Reserved status for the stage where the AI service is actively processing the scan                              |
| `COMPLETED`  | The inference completed successfully; the annotated image, detection metadata, and confidence scores were saved |
| `FAILED`     | Processing failed because of an error during image decoding, inference, storage access, or event handling       |

---

## 🛡 Validation and Error Handling Standards

The backend provides a consistent error structure for REST API clients (the returned JSON format includes `timestamp`, `status`, `error`, `message`, `path`, and a `validationErrors` map).

| Test Case             | Returned HTTP Status    | Description                                                               |
| :-------------------- | :---------------------- | :------------------------------------------------------------------------ |
| Missing or empty file | `400 Bad Request`       | Attempted to submit an empty form.                                        |
| Unsupported format    | `400 Bad Request`       | MIME type other than `image/jpeg` or `image/png`.                         |
| File too large        | `413 Payload Too Large` | Single scan size exceeds the strict limit of **10 MB**.                   |
| Non-existent patient  | `404 Not Found`         | Attempt to link an analysis to an ID that does not exist in the database. |

---

## 📨 RabbitMQ Messaging

The backend and AI worker communicate through the `mediscanflow.analysis` direct exchange.

**Queues:**

```text
analysis.requested
analysis.completed
analysis.failed
```

**Routing keys:**

```text
analysis.requested
analysis.completed
analysis.failed
```

**Workflow:**

1. The backend receives a scan upload request.
2. The original image is stored in MinIO.
3. Analysis metadata is stored in PostgreSQL.
4. An analysis request event is saved in the outbox table in the same transaction.
5. The scheduled outbox publisher reads pending events and publishes them to RabbitMQ.
6. The AI worker consumes the analysis request.
7. The worker downloads the original image from MinIO.
8. The worker runs YOLO inference and generates detection results.
9. The worker uploads the annotated image back to MinIO.
10. The worker publishes either a completed or failed event.
11. The backend consumes the result event and updates the analysis status.

---

## ⚙️ Configuration

The **Docker Compose** setup provides local development defaults through environment variables.

Common backend configuration:

```text
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mediscanflow
SPRING_RABBITMQ_HOST=rabbitmq
APP_STORAGE_MINIO_ENDPOINT=http://minio:9000
APP_STORAGE_MINIO_PUBLIC_ENDPOINT=http://localhost:9000
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8081/realms/mediscanflow
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/realms/mediscanflow/protocol/openid-connect/certs
```

Common web application configuration:

```text
NEXT_PUBLIC_API_BASE_URL=/api/backend
BACKEND_INTERNAL_URL=http://medical-platform-service:8080/api
NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8081
NEXT_PUBLIC_KEYCLOAK_REALM=mediscanflow
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=mediscanflow-web
```

Common AI worker configuration:

```text
RABBITMQ_HOST=rabbitmq
MINIO_ENDPOINT=minio:9000
MINIO_BUCKET=medical-scans
MODEL_PATH=models/yolov8n-brain-tumor.pt
YOLO_CONFIDENCE_THRESHOLD=0.25
SIMULATE_INFERENCE_FAILURE=false
```

---

## 🧪 Running Services Locally Without Docker

### Backend

From the repository root:

```bash
make backend-dev
```

Or manually:

```bash
cd services/medical-platform-service
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Run backend tests:

```bash
make backend-test
```

### Web Application

```bash
cd services/web-app
pnpm install
pnpm next dev
```

Useful commands:

```bash
pnpm next build
pnpm start
pnpm lint
pnpm typecheck
```

---

## 📝 Development Notes

- Backend code is organized by feature.
- REST API DTOs use the `DTO` suffix.
- Messaging event contracts do not use the `DTO` suffix because they represent integration event contracts.
- The frontend uses feature-based folders under `src/features`.
- The Next.js app proxies backend requests through `/api/backend/*`.
- Keycloak realm definitions are stored under `infra/keycloak`.
- Local credentials and client secrets are included only for development convenience.

## 🔮 Roadmap (Planned Improvements)

The project is developing iteratively. Upcoming milestones include:

1. **Full Support for DICOM Files** - implementing parsing for the native format used in medical diagnostic equipment (X-ray, CT, MRI) instead of standard consumer images.

2. **OpenAPI / Swagger Documentation** - integration with Springdoc-openapi to automatically generate and visualize interactive documentation for backend API endpoints.

3. **Testcontainers Integration Tests** - automating integration tests in the CI pipeline using the Testcontainers library (automatically spinning up PostgreSQL and RabbitMQ instances for testing).

4. **Observability Improvements** - enhanced logs, health checks, metrics, and monitoring readiness.
