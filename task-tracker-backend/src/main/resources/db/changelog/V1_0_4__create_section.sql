CREATE TABLE section
(
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    board_id BIGINT       NOT NULL REFERENCES board (id) ON DELETE CASCADE,
    created  TIMESTAMP DEFAULT now(),
    updated  TIMESTAMP,
    CONSTRAINT uk_section_name_board_id UNIQUE (name, board_id)
);