-- Les comptes existants sont considérés comme confirmés (DEFAULT TRUE) ; l'application crée les nouveaux à FALSE.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN sessions_valid_after TIMESTAMPTZ;

CREATE TABLE user_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(20)  NOT NULL,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL
);
