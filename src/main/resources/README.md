# Run profiles and environments

This backend defines three Spring profiles to support local development and production:

- dev (default): in-memory H2 for fast UI/backend development; data resets on restart.
- local: PostgreSQL with Flyway migrations for day-to-day database development locally.
- prod: PostgreSQL with Flyway migrations and production-safe settings.

## Environment variables (PostgreSQL profiles)

For profiles that connect to PostgreSQL (local, prod), set the following environment variables in the terminal before starting the application.

Windows cmd (temporary for current window):

```cmd
set DB_URL=jdbc:postgresql://localhost:5432/arkve
set DB_USER=ave_user
set DB_PASS=YourStrongPassword
```

Windows cmd (persist for new windows):

```cmd
setx DB_URL "jdbc:postgresql://localhost:5432/arkve"
setx DB_USER "ave_user"
setx DB_PASS "YourStrongPassword"
```

Note: after using `setx`, open a new terminal to see the values.

## How to run

- dev (H2, default)
	- Command:
		```cmd
		mvn spring-boot:run
		```
	- H2 console:
		- URL: http://localhost:8080/h2-console
		- JDBC: `jdbc:h2:mem:ave`
		- User: `sa`, Password: (blank)

- local (PostgreSQL + Flyway)
	- Ensure environment variables are set (see above), then run:
		```cmd
		mvn spring-boot:run -Dspring-boot.run.profiles=local
		```

- prod (PostgreSQL + Flyway)
	- Ensure environment variables are set (see above), then run:
		```cmd
		mvn spring-boot:run -Dspring-boot.run.profiles=prod
		```

## Flyway and JPA

- Flyway is enabled for `local` and `prod` profiles and records migrations in the `flyway_schema_history` table.
- Baseline-on-migrate is configured (version `1`) so an existing schema is accepted; new changes begin at `V2__*.sql` in `src/main/resources/db/migration`.
- JPA is set to `hibernate.ddl-auto=validate` in `local` and `prod`; schema changes are applied via Flyway migrations only.
- The `dev` profile uses H2 and does not run Flyway by default.

## Troubleshooting

- Error: `'url' must start with "jdbc"`
	- Cause: `DB_URL` not set or not visible in the terminal.
	- Fix: set environment variables in the same terminal before running.

- No mapper bean (e.g., `EventMapper`) found
	- Cause: annotation processing has not generated MapStruct implementations yet.
	- Fix: run a compile before starting:
		```cmd
		mvn clean compile -DskipTests
		```

