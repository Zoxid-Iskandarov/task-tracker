CREATE TABLE task_assignee
(
    task_id BIGINT NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, user_id)
);