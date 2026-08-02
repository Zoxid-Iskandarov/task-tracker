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

-- Пользователь 3: john_snow
-- Пароль: JohnSnow123
INSERT INTO users (id, username, email, password)
VALUES (3,
        'john_snow',
        'john.snow@example.com',
        '$2a$12$bzD0oqGqXXqTVmfNdMS1LeOCJWv4ZlXzzghK0CmacqZZf1EZMUSAO');

INSERT INTO user_profile (user_id, display_name, avatar_url, bio, created, updated)
VALUES (3,
        'John Snow',
        'https://example.com/avatars/snow.jpg',
        'Java Dev',
        NOW(),
        NOW());

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

INSERT INTO board (id, name, created, updated)
VALUES (1, 'Test Board', NOW(), NOW());

SELECT setval('board_id_seq', (SELECT MAX(id) FROM board));

INSERT INTO board_member (board_id, user_id, role, joined)
VALUES (1, 2, 'OWNER', NOW()),
       (1, 3, 'EDITOR', NOW());

INSERT INTO section (id, name, board_id, created, updated)
VALUES (1, 'To Do', 1, NOW(), NOW());

SELECT setval('section_id_seq', COALESCE((SELECT MAX(id) FROM section), 0));

-- Задачи с assignee
-- Task 1: два assignee (john_doe + jane_smith)
-- Task 2: один assignee (john_snow)
-- Task 3: без assignee (для проверки пустого списка)
INSERT INTO task (id, title, description, is_completed, position, section_id, created, updated)
VALUES (1, 'Test Task With Two Assignees', 'Description 1', FALSE, 1.0, 1, NOW(), NOW()),
       (2, 'Test Task With One Assignee', 'Description 2', FALSE, 2.0, 1, NOW(), NOW()),
       (3, 'Test Task Without Assignees', 'Description 3', FALSE, 3.0, 1, NOW(), NOW());

SELECT setval('task_id_seq', COALESCE((SELECT MAX(id) FROM task), 0));

INSERT INTO task_assignee (task_id, user_id)
VALUES (1, 1),  -- john_doe
       (1, 2),  -- jane_smith
       (2, 3);  -- john_snow