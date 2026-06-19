CREATE TABLE task_attachment
(
    id           BIGSERIAL PRIMARY KEY,
    task_id      BIGINT       NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    uploaded_by  BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    file_name    VARCHAR(255) NOT NULL,
    file_path    VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size    BIGINT       NOT NULL,
    created      TIMESTAMP DEFAULT now()
);