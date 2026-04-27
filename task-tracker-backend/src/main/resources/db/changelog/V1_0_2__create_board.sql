CREATE TABLE board
(
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    created TIMESTAMP DEFAULT now(),
    updated TIMESTAMP
);