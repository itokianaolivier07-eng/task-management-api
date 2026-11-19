-- PostgreSQL n'indexe pas automatiquement les clés étrangères : on les indexe pour les requêtes courantes.
CREATE INDEX IF NOT EXISTS idx_tasks_project         ON tasks (project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assignee        ON tasks (assignee_id);
CREATE INDEX IF NOT EXISTS idx_tasks_due_pending     ON tasks (due_date) WHERE reminder_sent = FALSE;
CREATE INDEX IF NOT EXISTS idx_comments_task         ON comments (task_id);
CREATE INDEX IF NOT EXISTS idx_members_user          ON workspace_members (user_id);
CREATE INDEX IF NOT EXISTS idx_projects_workspace    ON projects (workspace_id);
CREATE INDEX IF NOT EXISTS idx_user_tokens_expires   ON user_tokens (expires_at);
