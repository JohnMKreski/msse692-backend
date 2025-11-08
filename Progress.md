# Progress Report

---

**Date:** 10/11/2025

**Branch:** restAPI-service

**Layer:** Service Implementation

---

#### Work Completed

* Refactored and standardized the `EventService` interface (organized by commands and queries).
* Implemented and cleaned the `EventServiceImpl` class with structured business logic.
* Added SLF4J logging for all service operations (create, update, publish, unpublish, cancel, delete, and queries).
* Created `logback-spring.xml` for colored console logs and rolling file logs.
* Implemented the `EventStatus` enum with `DRAFT`, `PUBLISHED`, and `CANCELLED` values and readable display names.
* Updated `Event` entity to default to `DRAFT` without null checks in service logic.
* Refactored query methods for consistent naming, transaction handling, and logging.
* Applied `@Transactional(readOnly = true)` to all read-only methods.

---

#### TODO

* [x] Refactor the Mapper layer to align DTO-to-Entity conversions and include `statusDisplayName` mapping.
* [x] Implement repository methods: `findBySlug`, `findByTypeIgnoreCase`, `findByStartAtBetween`, and `findByLocationContainingIgnoreCase`.
* [**TODO**] Add unit and integration tests for the service layer.
* [x] Verify logging configuration across environments.
* [x] Begin Controller layer implementation once Mapper and Repository are complete.

---

## **Progress Report – Service & Controller Layer Development**

**Date:** October 13, 2025

### **Work Completed**

* **Service Layer**

    * Implemented and refined `EventServiceImpl` methods for full CRUD and state management (`create`, `update`, `delete`, `getAll`, etc.).
    * Standardized all date/time handling to use `LocalDateTime startAt` and `endAt` consistently across DTOs, entities, and repositories.
    * Rewrote query methods (`getEventsByType`, `getEventsByDate`) for clarity and proper enum/date logic.
    * Added structured SLF4J logging (`log.debug`, `log.info`, `log.error`) throughout service methods for traceability.
    * Refactored `EventStatus` and `EventType` enums for cleaner, display-friendly design.
    * Updated `EventDetailDto` and `EventDto` to align with entity and enum updates, including display name properties.
    * Refactored `EventMapper` for DTO/entity alignment and null-safe enum mapping.
    * Cleaned repository method signatures to match entity properties (`startAt`, `endAt`, etc.).
    * Verified console and file logging configuration.

* **Controller Layer**

    * Created `EventsController` with endpoints for:

        * `POST /api/events` – Create event (returns `201 Created` with `Location` header).
        * `GET /api/events/{id}` – Retrieve single event by ID.
        * `PUT /api/events/{id}` – Update existing event.
        * `DELETE /api/events/{id}` – Remove event.
    * Implemented `ServletUriComponentsBuilder` for REST-compliant resource URIs.
    * Used `Optional.map()` for elegant 404 handling.
    * Verified endpoint behavior with a working `MockMvc` unit test using `@WebMvcTest` and `@MockitoBean`.
    * Fixed Spring Security test lockout by disabling filters (`@AutoConfigureMockMvc(addFilters = false)`), confirming clean test execution.
    * Adjusted project to compile and run under **JDK 21**, resolving `javac 24 TypeTag::UNKNOWN` build error.

* **Environment & Swagger Fix**

    * Resolved runtime `NoSuchMethodError: ControllerAdviceBean.<init>` by upgrading **springdoc** from `2.5.0` → `2.8.13`, matching **Spring Boot 3.5.6 / Spring 6.2.x**.
    * Verified successful startup of backend under **JDK 21**, no compiler or classpath conflicts.
    * Confirmed **Swagger UI loads correctly** at `http://localhost:8080/scalar` (OpenAPI endpoint → `/v3/api-docs`).
    * System is now stable and ready for controller integration testing and upcoming database connection work.

### **Outstanding Tasks (Next Steps)**

* [x] Verify successful application startup under JDK 21.
* [x] Create and pass initial unit test for `EventsController`.
* [ ] Add additional controller tests (`GET`, `PUT`, `DELETE`) for coverage.
* [ ] Confirm log formatting and rotation in runtime.
* [ ] Begin integration tests once a database layer (e.g., PostgreSQL) is added.
* [ ] Extend repository slice tests for `findByEventType` and `findByStartAtBetween`.


--- 

11/05/2025

• SQL: Connected local Postgres (env vars), enabled Flyway; added V2 migration for app_users and app_user_roles; verified tables and seeded roles via psql; added psql-cheatsheet.md.
• Firebase: Configured Spring Security OAuth2 Resource Server with Firebase issuer/audience; Angular interceptor sends ID token; added /api/auth/whoami to inspect claims.
• User: Created AppUser entity/repo and upsert filter to create/update user on first authenticated request; roles resolved from JWT or DB (default USER).
• User controller: Added admin-only endpoints to manage roles:
    - GET /api/admin/users/{uid}/roles
    - POST /api/admin/users/{uid}/roles (add roles)
    - DELETE /api/admin/users/{uid}/roles/{role}
    Protected by ROLE_ADMIN.
• Security: Public GETs for /api/events/** and /api/enums/**; write endpoints guarded (ADMIN/EDITOR for create/update, ADMIN for delete).


11/07/2025

• Frontend Profile Page: Structured into explicit sections (Firebase info JSON, Backend AppUser JSON, Profile data, My Events filtered by createdByUserId, All Events dev visibility, Admin-only Event Audits).
• Firebase Transparency: Displayed all user fields including nulls; token claims surfaced; roles fallback to backend AppUser when missing in JWT.
• Event Ownership: Added createdByUserId and lastModifiedByUserId via JPA auditing annotations (@CreatedBy/@LastModifiedBy) on `Event`; exposed in `EventDto` and `EventDetailDto`.
• Auditing Infrastructure: Implemented `event_audit` table + `EventAuditService` with logCreate/logUpdate/logDelete; added GET /api/events/{id}/audits endpoint and Angular service + UI section (admin-only) showing recent actions with relative times.
• Frontend Enhancements: Added event normalization (id/title/start/end/location) and filtering logic; replaced heavy inline CRUD form with placeholder button + toast (event management to move to dedicated page).
• Security & Profiles: Kept Flyway enabled for local/prod (PostgreSQL) and disabled Flyway in dev/test (H2) to avoid TIMESTAMPTZ/BIGSERIAL incompatibility; dev relies on Hibernate `ddl-auto=update`.
• StackOverflowError Fix (PUT /api/events): Eliminated recursive auditing by removing Lombok @Data on `Event` (granular annotations only) and introducing ThreadLocal `CurrentAuditor`; refactored `AuditorAware` to use ThreadLocal instead of repository lookup during flush.
• Tests Stabilization: Adjusted test profile (`application-test.yml`) to disable Flyway; updated `ProfileIntegrationTest` to use `test` profile with Hibernate schema; cleaned unused injections/imports.
• Logging Refinement: Corrected misleading publish/update log messages; ensured event update logs status without triggering recursion.
• Misc: Normalized event selection for audits (fallback to all events if user has none) and ensured selectedEventId initializes promptly to load audits.
• Result: CRUD (create, list, update, delete) and auditing fully operational across dev/local; PUT works with ADMIN token; no further StackOverflow errors observed.


### Safe migrations (policy)

- Never edit an applied migration. If a change is needed, add a new versioned file (V{n+1}__short_description.sql) rather than altering Vn files.
- Profiles:
    - Dev/Test (H2): Flyway disabled; Hibernate `ddl-auto=update` manages schema. Don’t use H2 to validate Postgres-specific types.
    - Local/Prod (Postgres): Flyway enabled; migrations are the sole source of schema truth.
- Baseline/attach: When pointing Flyway at an existing DB, set a baseline version and migrate forward; don’t “clean” a shared database.
- Repair usage: Use “repair” only to fix metadata after a failed or partially applied migration you’ve verified. Prefer writing a new migration over editing a checksumed file.
- Authoring guidelines:
    - Keep migrations small, ordered, and clearly named; avoid combining unrelated changes.
    - Prefer additive, backward-compatible changes (add column with default, backfill, then make NOT NULL) to reduce downtime/locking.
    - Avoid destructive operations (DROP/DELETE) unless absolutely necessary and only with backups and explicit approval.
    - Make backfills safe and chunked if touching many rows; avoid long locks.
- Validation:
    - Test migrations end-to-end on a fresh Postgres instance before merging.
    - Don’t rely on H2 behavior to predict Postgres behavior.
- Recovery playbook:
    1) Dev-only failure: bump version with a corrected migration and rerun; don’t edit the faulty file.
    2) Checksum mismatch locally: revert your file to the applied version or run a targeted repair after confirming SQL parity.
    3) Need to revert: write forward “undo” migrations (don’t delete history) and then apply a fix-forward migration.

### Development (Angular on :4200, Spring Boot on :8080)

- Yes—keep using Angular’s dev proxy (`proxy.conf.json`). It:
    - Avoids CORS headaches
    - Lets you keep a relative API base path like `/api` in the app
    - Keeps local URLs simple and consistent
- Make sure you actually start the dev server with the proxy config so requests don’t bypass it.

### Production

- Don’t rely on the Angular dev proxy. Instead:
    - Best: serve the frontend and backend under the same domain and use an edge reverse proxy (NGINX/Traefik/API Gateway) to route:
        - `/` → frontend (static assets or SSR)
        - `/api` → Spring Boot
    - Benefits: no CORS, simpler security, cookies work if you ever need them, consistent origin
- If you must host on separate domains:
    - Configure CORS on Spring Boot (allowed origins, methods, headers including `Authorization`)
    - Use an absolute API URL in Angular’s environment files for prod
    - Keep auth via `Authorization: Bearer <token>` (ensure CORS allows that header)
- SSR note (if you enable Angular Universal):
    - You can still keep the same-origin model via the reverse proxy. The SSR node server and backend can be routed behind the same domain.