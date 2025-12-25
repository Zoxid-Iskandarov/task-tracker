INSERT INTO users (id, username, email, password)
VALUES (1, 'Zoxka', 'san781617@gmail.com',
        '$2a$10$YYaAaFP54K74pbaXJj36d.KlNcBKXTwqT71qEndV.hSGc5TVDTsdS'), --Zox617
       (2, 'Ivan', 'ivan99@gmail.com',
        '$2a$10$EaZaBaEkswJl939P1D.iJ.uY7yOA0X.XP5/ojImDx.h9..xCudCDW'); --Ivan99

SELECT setval('users_id_seq', 2);

INSERT INTO task (id, title, description, is_completed, created, updated, user_id)
VALUES (1, 'Implement user registration',
        'Create /auth/sign-up endpoint with validation and database persistence',
        TRUE, now(), now(), 1),
       (2, 'Configure Redis session storage',
        'Integrate Spring Session with Redis and verify session lifecycle',
        TRUE, now(), now(), 1),
       (3, 'Add entity auditing',
        'Enable created and updated timestamps using Spring Data auditing',
        TRUE, now() - INTERVAL '2 days', now() - INTERVAL '2 days', 1),
       (4, 'Implement password recovery',
        'Send reset password email with token and implement reset form',
        FALSE, now() - INTERVAL '2 days', now() - INTERVAL '2 days', 1),
       (5, 'Optimize image upload',
        'Add avatar preview and file size validation on upload',
        FALSE, now(), now(), 1),
       (6, 'Create login page UI',
        'Build login form using HTML and Bootstrap 5 with error handling',
        TRUE, now(), now(), 2),
       (7, 'Integrate Kafka for email notifications',
        'Publish email events to Kafka and consume them in email sender service',
        TRUE, now(), now(), 2),
       (8, 'Implement tasks REST API',
        'Create CRUD endpoints for tasks with DTOs and validation',
        TRUE, now() - INTERVAL '2 days', now() - INTERVAL '2 days', 2),
       (9, 'Add task filtering',
        'Filter tasks by status and creation date',
        FALSE,now() - INTERVAL '2 days',now() - INTERVAL '2 days',2),
       (10, 'Write service layer tests',
        'Add unit and integration tests for task services',
        FALSE, now(), now(), 2);

SELECT setval('task_id_seq', 10);

