# psql shell quick reference (Windows + general)

This doc summarizes common psql commands to navigate, inspect, and update your PostgreSQL database from the interactive shell.

Use these commands after you are connected to psql. Lines starting with a backslash (\) are psql meta-commands; others are SQL statements.

## Connect to Postgres (Windows cmd.exe)

- Prompted mode:
  - `psql -h localhost -p 5432 -U <user> -d <database>`
- Without password prompt (set for this terminal only):
  - `set PGPASSWORD=<your_password>`
  - `psql -h localhost -p 5432 -U <user> -d <database>`
- Quick sanity check once connected:
  - `SELECT current_user, current_database(), version();`

Tip: On Windows, environment variables are set per-terminal. Close/reopen or re-run `set` to update.

## Where am I? Connection info

- `\conninfo` — show current connection details
- `SELECT current_user, current_database();`

## Databases, schemas, tables

- `\l` — list databases
- `\c <dbname>` — connect/switch to a database
- `\dn` — list schemas
- `\dt` — list tables in current schema (usually `public`)
- `\dt public.*` — list tables in the `public` schema
- `\dv` — list views
- `\dx` — list installed extensions
- `\du` — list roles/users

## Inspect a table

- `\d app_users` — show columns, indexes, constraints
- `\d+ app_users` — include storage/details
- `\d app_user_roles`

## Query examples (Ark Valley Events app)

Find your user:

- By Firebase UID:
  - `SELECT id, firebase_uid, email, display_name FROM app_users WHERE firebase_uid = '<your_firebase_uid>';`
- By email:
  - `SELECT id, firebase_uid, email, display_name FROM app_users WHERE email = 'you@example.com';`

See your current roles (replace `<user_id>`):

- `SELECT user_id, role FROM app_user_roles WHERE user_id = <user_id>;`

Grant roles (store without `ROLE_` prefix; backend maps to `ROLE_*`):

- Insert ADMIN and EDITOR (safe to re-run):
  - `INSERT INTO app_user_roles (user_id, role)`
  - `VALUES (<user_id>, 'ADMIN'), (<user_id>, 'EDITOR')`
  - `ON CONFLICT DO NOTHING;`

Remove a role:

- `DELETE FROM app_user_roles WHERE user_id = <user_id> AND role = 'EDITOR';`

## Output and UX helpers

- `\x` — toggle expanded display (useful for wide rows)
- `\timing on` — show execution time for queries
- `\pset pager off` — disable paging (print all output)
- `\copy (SELECT ...) TO 'C:/path/out.csv' CSV HEADER` — export query to CSV

## Transactions (optional safety)

- `BEGIN;` — start a transaction
- Run your statements
- `ROLLBACK;` — undo changes (or `COMMIT;` to save)

## Help and exit

- `\?` — help for psql meta-commands
- `\h` — help for SQL commands (e.g., `\h insert`)
- `\q` — quit psql

## Common errors and fixes

- `FATAL: password authentication failed for user ...`
  - Wrong username/password; verify credentials. If your app connects successfully, use the same `DB_USER`/`DB_PASS`.
- `FATAL: database "..." does not exist`
  - Create it or connect to an existing database.
- `permission denied` / `relation does not exist`
  - Ensure you are connected to the correct database and schema, and your user has privileges.

## Windows cmd.exe notes

- Line continuation: use `^` at the end of a line
- Avoid stray spaces in environment variable values (e.g., `DB_URL` must start exactly with `jdbc:`)
- Variables expand with `%VAR%` (e.g., `%DB_USER%`)

---

For the Ark Valley Events backend specifically, the most common flow is:

1) `\dt` to see tables
2) `SELECT id FROM app_users WHERE firebase_uid = '<uid>';`
3) `INSERT INTO app_user_roles (user_id, role) VALUES (<id>, 'ADMIN') ON CONFLICT DO NOTHING;`
4) `SELECT * FROM app_user_roles WHERE user_id = <id>;`
5) Test your authorized endpoints with a fresh Firebase ID token.
