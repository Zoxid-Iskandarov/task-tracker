CREATE TABLE task_comment
(
    id        BIGSERIAL PRIMARY KEY,
    task_id   BIGINT    NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    author_id BIGINT    REFERENCES users (id) ON DELETE SET NULL,
    content   TEXT      NOT NULL,
    created   TIMESTAMP NOT NULL DEFAULT now(),
    updated   TIMESTAMP NOT NULL
);