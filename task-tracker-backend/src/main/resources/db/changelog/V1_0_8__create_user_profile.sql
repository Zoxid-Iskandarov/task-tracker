CREATE TABLE user_profile
(
    user_id      BIGINT PRIMARY KEY REFERENCES users (id),
    display_name VARCHAR(100),
    avatar_url   VARCHAR(500),
    bio          TEXT,
    created      TIMESTAMP NOT NULL,
    updated      TIMESTAMP NOT NULL
);