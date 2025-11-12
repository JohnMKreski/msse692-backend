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

## 11/08/2025 — Changes

### Backend

#### Firebase Admin integration
- Added FirebaseApp/FirebaseAuth beans.
- Implemented FirebaseClaimsSyncService (+ Impl): normalize roles, add USER if empty, compute `roles_version`, set custom claims, structured logs.

#### Admin endpoints
- GET `/api/admin/users/{uid}/roles`
- POST `/api/admin/users/{uid}/roles` (add; triggers sync)
- DELETE `/api/admin/users/{uid}/roles/{role}` (remove; triggers sync)
- POST `/api/admin/users/{uid}/roles/sync?force=bool`
- Controller logs actor, uid, changes.

#### Security
- `ProdSecurityConfig`: issuer/audience validation; map roles from JWT claims; fallback to DB roles if claim missing; default USER when empty; run `AppUserUpsertFilter` after auth.

#### Tests/observability
- Unit tests for claims sync normalization/default/error paths.
- Enhanced logs for sync attempts, success/failure, timing.

#### Deferred by design
- No DB-persisted roles hash or audit table for claim sync (logging-only).

### Frontend

#### Auth plumbing
- HTTP interceptor adds `Authorization: Bearer <ID token>`.
- Proxy routes `/api` → `http://localhost:8080`.

#### Test API page (`/test-api`)
- “Admin: Get Roles by UID” form; shows response.
- “Refresh Token” button (force `getIdToken(true)`).

#### Profile page (`/profile`)
- Section 1: shows full Firebase user JSON, Token Claims JSON, Roles with source (`firebase-claims` or `backend-fallback`).
- Decoded ID token header/payload displayed.
- “Refresh Token & Claims” button to re-fetch claims.

#### UX/validation
- Identified and fixed 401 root causes (placeholder UID, truncated token).
- Verified admin-only endpoints with Postman and in-app tools.

11/11/2025

Event Status:
Backend steps
Expose EventStatus via EnumsController

Add GET /api/v1/enums/event-statuses.
Response shape: an array of { code: "DRAFT" | "PUBLISHED" | "UNPUBLISHED" | "CANCELLED", displayName: string }.
Source displayName from EventStatus.getStatusDisplayName().
Note: Clean up the enum double semicolon in EventStatus if desired (CANCELLED(...);; → CANCELLED(...);) as a tiny hygiene fix.
Define status transition endpoints (if not already present)

POST /api/v1/events/{id}/publish → EventDetailDto
POST /api/v1/events/{id}/unpublish → EventDetailDto
POST /api/v1/events/{id}/cancel → EventDetailDto
These should delegate to EventServiceImpl.publishEvent/unpublishEvent/cancelEvent and return the updated event.
Enforce allowed transitions (state machine)

Allowed:
DRAFT → PUBLISHED
PUBLISHED → UNPUBLISHED
DRAFT | PUBLISHED | UNPUBLISHED → CANCELLED
Disallowed:
PUBLISHED → DRAFT, UNPUBLISHED → PUBLISHED (use publish to re-publish), CANCELLED → anything
On disallowed transitions, return 409 Conflict with standardized error body (your existing RestExceptionHandler schema).
Idempotency and error semantics

If publish is called on PUBLISHED or cancel on CANCELLED:
Either return 409 (preferred for clear UX) or make it idempotent no-op with 200; choose one and document it. Your current service throws IllegalStateException → map to 409.
Include requestId in responses via existing logging filter.
Visibility and listing rules

Public-facing listings must return only PUBLISHED events:
Add repository/service method for “public upcoming” (status=PUBLISHED, startAt >= now, ascending).
Keep an admin listing that can see all statuses (already present).
Validation and time rules

Optional: prevent publish if startAt/endAt are invalid (e.g., endAt < startAt) → 400 Bad Request.
Security and authorization

Require appropriate roles/authorities to perform publish/unpublish/cancel (e.g., ROLE_ADMIN or event owner).
Leave GET status enums and public listings as permitAll.
Add unit tests for authorization behavior or mock out security in controller tests.
Logging and auditing

Reuse EventAuditService to logUpdate on publish/unpublish/cancel and logCreate/logDelete elsewhere.
Include eventId, previousStatus → newStatus in logs (already logged in EventServiceImpl).
OpenAPI docs

Document the enums endpoint and the three transition endpoints with request/response schemas and possible 409 error.
Tests

Unit tests (service): happy paths for each transition + negative cases (IllegalStateException → 409).
Controller tests: ensure endpoints hit the service and return the correct status codes and payloads.
Enum endpoint test: returns all statuses with correct display names.
Frontend steps
Model and enum retrieval

Add a small interface: { code: 'DRAFT' | 'PUBLISHED' | 'UNPUBLISHED' | 'CANCELLED'; displayName: string }.
In a shared enums service, add getEventStatuses() calling /api/v1/enums/event-statuses.
Cache in-memory (e.g., via shareReplay(1)) to avoid repeated calls.
Event status in UI

Ensure EventDto/EventDetailDto use backend-provided status and statusDisplayName.
For admin/editor pages, show the displayName; for logic, use code (strict comparisons).
Action buttons for transitions

On the event detail/admin edit screen:
Show Publish when status === DRAFT.
Show Unpublish when status === PUBLISHED.
Show Cancel when status !== CANCELLED.
Disable buttons while in-flight; show tooltips explaining why a button is disabled.
Call transition endpoints

Add methods to event service:
publishEvent(id): POST /api/v1/events/{id}/publish
unpublishEvent(id): POST /api/v1/events/{id}/unpublish
cancelEvent(id): POST /api/v1/events/{id}/cancel
On success: update local event state or refetch the event; show success toast/snackbar.
On error (409): show a friendly message (e.g., “This action isn’t valid for the current status”).
Public views filtering

Public event lists should call a service method that only returns PUBLISHED (and optionally upcoming-only).
Admin views can toggle filters to include DRAFT/UNPUBLISHED/CANCELLED.
Routing and guards

Public detail pages (by slug) should handle non-PUBLISHED events:
Show 404/not found or a friendly message if event is not PUBLISHED.
Admin route can still view non-published details if authorized.
Accessibility and UX

Add confirmation dialog for Cancel action.
Provide clear status badges (colors/icons) with accessible text (aria-labels).
Error handling

Use your global HTTP interceptor to normalize errors; map 409 from backend to actionable UI messages.
If rate limits or duplicates occur, present retry or guidance.
Tests

Unit tests for enums service (caching behavior, happy/error).
Component tests: buttons render per status, call service on click, handle success/error states.
Integration/e2e smoke: publish → unpublish → cancel basic flows.
Contract and success criteria
Inputs:
Event ID for transitions; no body required.
No body for enum fetch.
Outputs:
Transition endpoints return updated EventDetailDto.
Enum endpoint returns array of { code, displayName }.
Error modes:
404 for missing event.
409 for invalid transition.
403 for unauthorized.
Success criteria:
Public lists show only PUBLISHED events.
Admins can publish/unpublish/cancel per state machine.
UI clearly reflects current status and available actions.