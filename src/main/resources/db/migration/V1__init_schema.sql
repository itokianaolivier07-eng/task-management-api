-- Schéma initial (équivalent des entités JPA). Appliqué par Flyway dans le profil « prod ».
-- En développement, Hibernate (ddl-auto=update) gère le schéma et Flyway est désactivé.

CREATE TABLE users (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE workspaces (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    owner_id    BIGINT NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP(6)
);

CREATE TABLE workspace_members (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces (id),
    user_id      BIGINT NOT NULL REFERENCES users (id),
    role         VARCHAR(255) NOT NULL,
    CONSTRAINT uk_workspace_members_workspace_user UNIQUE (workspace_id, user_id)
);

CREATE TABLE projects (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(2000),
    workspace_id  BIGINT NOT NULL REFERENCES workspaces (id),
    created_by_id BIGINT NOT NULL REFERENCES users (id),
    created_at    TIMESTAMP(6)
);

CREATE TABLE tasks (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   VARCHAR(4000),
    status        VARCHAR(255) NOT NULL,
    priority      VARCHAR(255) NOT NULL,
    due_date      DATE,
    project_id    BIGINT NOT NULL REFERENCES projects (id),
    assignee_id   BIGINT REFERENCES users (id),
    created_by_id BIGINT NOT NULL REFERENCES users (id),
    created_at    TIMESTAMP(6),
    reminder_sent BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE comments (
    id         BIGSERIAL PRIMARY KEY,
    task_id    BIGINT NOT NULL REFERENCES tasks (id),
    author_id  BIGINT NOT NULL REFERENCES users (id),
    content    VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP(6)
);
