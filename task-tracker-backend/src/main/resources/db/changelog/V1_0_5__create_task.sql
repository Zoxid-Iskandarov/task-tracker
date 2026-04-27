CREATE TABLE task
(
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(255)     NOT NULL,
    description  TEXT,
    is_completed BOOLEAN   DEFAULT FALSE,
    position     DOUBLE PRECISION NOT NULL,
    section_id   BIGINT           NOT NULL REFERENCES section (id) ON DELETE CASCADE,
    created      TIMESTAMP DEFAULT now(),
    updated      TIMESTAMP
);