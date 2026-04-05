CREATE TABLE board
(
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    user_id BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created TIMESTAMP DEFAULT now(),
    updated TIMESTAMP,
    CONSTRAINT uk_board_name_user_id UNIQUE (name, user_id)
);