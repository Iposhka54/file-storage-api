CREATE TABLE IF NOT EXISTS user_action_audit
(
    id          UUID PRIMARY KEY,
    username    VARCHAR(32) NOT NULL,
    action      VARCHAR(255) NOT NULL,
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);