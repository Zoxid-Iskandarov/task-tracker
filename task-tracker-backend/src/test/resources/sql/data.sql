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

-- Board 1: jane_smith (2) - OWNER; john_snow (3) - EDITOR
-- Board 2: john_doe (1) - OWNER, jane_smith (2) - VIEWER
INSERT INTO board (id, name, created, updated)
VALUES (1, 'Test Board', NOW(), NOW()),
       (2, 'Second Test Board', NOW(), NOW()),
       (3, 'Third Board', NOW(), NOW());

SELECT setval('board_id_seq', (SELECT MAX(id) FROM board));

INSERT INTO board_member (board_id, user_id, role, joined)
VALUES (1, 2, 'OWNER', NOW()),
       (1, 3, 'EDITOR', NOW()),
       (2, 1, 'OWNER', NOW()),
       (2, 2, 'VIEWER', NOW()),
       (3, 2, 'OWNER', NOW());

INSERT INTO section (id, name, board_id, created, updated)
VALUES (1, 'To Do', 1, NOW(), NOW()),
       (2, 'In Progress', 1, NOW(), NOW());

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
VALUES (1, 1), -- john_doe
       (1, 2), -- jane_smith
       (2, 3); -- john_snow

INSERT INTO label (id, name, colour, board_id)
VALUES (1, 'Bug', 'RED', 1),
       (3, 'Board 3 Label', 'GREEN', 3);

SELECT setval('label_id_seq', COALESCE((SELECT MAX(id) FROM label), 0));

INSERT INTO task_comment (id, task_id, author_id, content, created, updated)
VALUES (1, 1, 1, 'Test comment content', NOW(), NOW());

SELECT setval('task_comment_id_seq', COALESCE((SELECT MAX(id) FROM task_comment), 0));

INSERT INTO user_activity (id, user_id, username, board_id, board_name, activity_type, description, created)
VALUES (1, 2, 'jane_smith', 1, 'Test Board', 'BOARD_CREATED', 'Created board Test Board', NOW()),
       (2, 3, 'john_snow', 1, 'Test Board', 'MEMBER_ADDED', 'Added member john_snow', NOW()),
       (3, 1, 'john_doe', 2, 'Second Test Board', 'BOARD_CREATED', 'Created board Second Test Board', NOW());

SELECT setval('user_activity_id_seq', COALESCE((SELECT MAX(id) FROM user_activity), 0));

INSERT INTO task_attachment (id, task_id, uploaded_by, file_name, file_path, content_type, file_size, created)
VALUES (1, 1, 1, 'spec.pdf', 'attachments/1/spec.pdf', 'application/pdf', 2048, NOW());

SELECT setval('task_attachment_id_seq', COALESCE((SELECT MAX(id) FROM task_attachment), 0));