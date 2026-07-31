-- Пользователь 1: john_doe
-- Пароль: password123
INSERT INTO users (id, username, email, password)
VALUES (1,
        'john_doe',
        'john.doe@example.com',
        '$2a$12$Y0nwjC.OjSWTUxNVnGvbfudznHKI.gctraIcMJMmDT9SRcirrv00m');

INSERT INTO user_profile (user_id, display_name, avatar_url, bio, created, updated)
VALUES (1,
        'John Doe',
        'https://example.com/avatars/john.jpg',
        'Software engineer from California. Love hiking and coding.',
        NOW(),
        NOW());

-- Пользователь 2: jane_smith
-- Пароль: strongPass456
INSERT INTO users (id, username, email, password)
VALUES (2,
        'jane_smith',
        'jane.smith@example.com',
        '$2a$12$tPvdfT.Pv/GoZ7//eiZZmONK//NEtMJdE3DgfjTbTf065X3cYrbY2');

INSERT INTO user_profile (user_id, display_name, avatar_url, bio, created, updated)
VALUES (2,
        'Jane Smith',
        'https://example.com/avatars/jane.jpg',
        'Product manager. Coffee addict ☕',
        NOW(),
        NOW());

SELECT setval('users_id_seq', 2)
