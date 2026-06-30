CREATE TABLE analysis_detections
(
    id              UUID PRIMARY KEY,
    analysis_id     UUID             NOT NULL,
    detection_label VARCHAR(100)     NOT NULL,
    confidence      DOUBLE PRECISION NOT NULL,
    x_coordinate    DOUBLE PRECISION NOT NULL,
    y_coordinate    DOUBLE PRECISION NOT NULL,
    width           DOUBLE PRECISION NOT NULL,
    height          DOUBLE PRECISION NOT NULL,

    CONSTRAINT fk_analysis_detections_analysis
        FOREIGN KEY (analysis_id)
            REFERENCES analyses (id)
            ON DELETE CASCADE
);