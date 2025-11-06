# Flyway quick guide (baseline setup)

Flyway manages database schema changes with versioned SQL files kept alongside the application code.

## Responsibilities and behavior

- Purpose: apply schema/data changes in a controlled, versioned manner.
- Default discovery location: `classpath:db/migration` (resolved at build time to `src/main/resources/db/migration`).
- Script naming: `V{version}__{description}.sql` (double underscore), for example:
	- `V1__init_schema.sql`
	- `V2__add_event_indexes.sql`
- Execution time: application startup; Spring Boot runs Flyway before JPA initializes.
- History tracking: applied migrations are recorded in the `flyway_schema_history` table.

## Configuration in this application

Under the `prod` profile in `application.yml`:

```yaml
spring:
	flyway:
		enabled: true
		baseline-on-migrate: true
		baseline-version: 1
		locations: classpath:db/migration
	jpa:
		hibernate.ddl-auto: validate
```

- Baseline-on-migrate: accepts the existing schema as version `1` without modifying tables.
- New migrations begin at `V2` and are applied incrementally on subsequent starts.
- The `dev` profile (H2) is not configured to run Flyway in this setup.

## Distinction from `@Version` (optimistic locking)

`@Version` on a JPA entity (e.g., `Event.version`) implements optimistic locking per row. This is unrelated to Flyway’s schema versioning, which is recorded globally in `flyway_schema_history`.

## Creating a new migration (example: V2)

1. Create a SQL file under `src/main/resources/db/migration`, e.g.:

	 `V2__add_useful_index.sql`

2. Add PostgreSQL statements, for example:

```sql
-- Add an index to speed up queries listing upcoming events
CREATE INDEX IF NOT EXISTS idx_event_start_at ON event (start_at);

-- Optional seed data
-- INSERT INTO event (slug, event_name, event_type, start_at, end_at, status, created_at, updated_at)
-- VALUES ('sample-show', 'Sample Show', 'CONCERT', '2026-01-10T19:00:00', '2026-01-10T21:00:00', 'DRAFT', NOW(), NOW());
```

3. Start the application with the `prod` profile; Flyway detects and applies `V2` automatically.

4. Verify application of the migration:

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank DESC;
```

The history table should contain an entry for `V2` with `success = true` after a successful run.

## Authoring guidance (PostgreSQL)

- Prefer `IF NOT EXISTS`/`IF EXISTS` to keep scripts re‑runnable during development.
- Avoid quoted identifiers in DDL; rely on unquoted, lowercase identifiers for consistency.
- Keep one logical change per migration; small, focused migrations are easier to review and deploy.
- Operations like `CREATE INDEX CONCURRENTLY` cannot run inside a transaction; configure a non‑transactional migration only when necessary and understood.

## Rollback strategy

Flyway is forward‑only. To reverse a change, author a subsequent migration (e.g., `V3__revert_index.sql` with `DROP INDEX IF EXISTS idx_event_start_at;`) or restore from a database backup/snapshot.

## Additional notes

- Only `.sql` files that match the `V{n}__*.sql` convention are executed; Markdown files (such as this document) are not executed.
- Additional locations can be provided via `spring.flyway.locations` (supports multiple classpath entries and `filesystem:` paths).

