CREATE TABLE user_activity
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    username      VARCHAR(100) NOT NULL,
    board_id      BIGINT       NOT NULL,
    board_name    VARCHAR(100) NOT NULL,
    activity_type VARCHAR(50)  NOT NULL,
    description   TEXT         NOT NULL,
    created       TIMESTAMP DEFAULT now()
);