-- App users to store application-specific profile and roles (no passwords)
CREATE TABLE IF NOT EXISTS app_users (
  id            BIGSERIAL PRIMARY KEY,
  firebase_uid  VARCHAR(128) NOT NULL UNIQUE,
  email         VARCHAR(320),
  display_name  VARCHAR(200),
  photo_url     TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_app_users_uid ON app_users(firebase_uid);

-- Roles as a separate collection table (portable across DBs)
CREATE TABLE IF NOT EXISTS app_user_roles (
  user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  role    VARCHAR(50) NOT NULL,
  PRIMARY KEY (user_id, role)
);
