CREATE TABLE analyses
(
    id                 UUID PRIMARY KEY,
    patient_id         UUID         NOT NULL,
    status             VARCHAR(30)  NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    object_key         VARCHAR(500) NOT NULL,
    content_type       VARCHAR(100),
    file_size_bytes    BIGINT       NOT NULL,
    model_name         VARCHAR(100),
    model_version      VARCHAR(50),
    error_message      TEXT,
    created_at         TIMESTAMP    NOT NULL,
    completed_at       TIMESTAMP,

    CONSTRAINT fk_analyses_patient
        FOREIGN KEY (patient_id)
            REFERENCES patients (id)
            ON DELETE CASCADE
);