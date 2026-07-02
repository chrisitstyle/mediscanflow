CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    type VARCHAR(64) NOT NULL,

    actor_user_id VARCHAR(128),
    actor_email VARCHAR(255),
    actor_role VARCHAR(64),

    patient_id UUID,
    analysis_id UUID,

    message VARCHAR(500) NOT NULL,
    metadata TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_events_created_at
    ON audit_events (created_at DESC);

CREATE INDEX idx_audit_events_patient_id_created_at
    ON audit_events (patient_id, created_at DESC);

CREATE INDEX idx_audit_events_analysis_id_created_at
    ON audit_events (analysis_id, created_at DESC);
