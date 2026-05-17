CREATE TABLE IF NOT EXISTS trash
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    path         TEXT      NOT NULL,
    name         TEXT      NOT NULL,
    is_directory BOOLEAN   NOT NULL,
    deleted_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
