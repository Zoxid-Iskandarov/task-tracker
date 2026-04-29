CREATE TABLE label
(
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(50) NOT NULL,
    colour   VARCHAR(10) NOT NULL,
    board_id BIGINT      NOT NULL REFERENCES board (id) ON DELETE CASCADE,
    CONSTRAINT uk_label_name_board_id UNIQUE (name, board_id)
);

CREATE TABLE tasks_labels
(
    task_id  BIGINT NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES label (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, label_id)
);