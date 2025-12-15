CREATE TABLE task
(
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    is_completed BOOLEAN   DEFAULT FALSE,
    created      TIMESTAMP DEFAULT now(),
    updated      TIMESTAMP,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE
);