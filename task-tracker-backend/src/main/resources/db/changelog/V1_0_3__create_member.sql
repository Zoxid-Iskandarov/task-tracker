CREATE TABLE board_member
(
    board_id BIGINT      NOT NULL REFERENCES board (id) ON DELETE CASCADE,
    user_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role     VARCHAR(10) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    joined   TIMESTAMP DEFAULT now(),
    PRIMARY KEY (board_id, user_id)
);