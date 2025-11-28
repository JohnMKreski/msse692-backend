# Backend Architecture Overview

This document provides a high‑level architectural summary of the `msse692-backend` Spring Boot application: layers, data flow, security model, key packages, and technology choices.

---
## 1. Technology Stack
- **Framework**: Spring Boot 3.5.x (Java 21)
- **Persistence**: Spring Data JPA (Hibernate)
- **Database**: PostgreSQL (primary) + H2 (runtime/dev/testing)
- **Migrations**: Flyway (core + PostgreSQL database module)
- **Security**: Spring Security + OAuth2 Resource Server (JWT from Firebase issuer)
- **API Docs**: springdoc-openapi (Swagger UI + Scalar UI)
- **Mapping**: MapStruct (DTO ↔ Entity mapping; annotation processor configured)
- **Boilerplate Reduction**: Lombok (getter/setter, equals/hashCode, etc.)
- **Auditing**: Spring Data auditing (@CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy)
- **Testing**: JUnit Jupiter, Spring Boot Test, Spring Security Test, Testcontainers (PostgreSQL) potential integration
- **Formatting**: Spotless (Google Java Format)
- **Cloud Integration**: Firebase Admin (for potential server-side operations; JWTs originate from Firebase auth)

---
## 2. Package Structure
```
com/arkvalleyevents/msse692_backend
├── Msse692BackendApplication.java          # Spring Boot entry point
├── config/                                 # Security & infrastructure configuration
├── controller/                             # REST API controllers (HTTP interface)
├── dto/                                    # Request/Response DTOs (API boundary models)
├── logging/                                # (Assumed) Logging utilities / interceptors
├── model/                                  # JPA entities (domain data)
├── repository/                             # Spring Data repositories (data access abstraction)
├── security/                               # Policy & user context classes
├── service/                                # Business services (interfaces & implementations)
├── util/                                   # Cross-cutting utilities (e.g. CurrentAuditor)
```

---
## 3. Layered Architecture & Data Flow
1. **Controller Layer** (`controller`): Accepts HTTP requests, performs validation (`@Valid`, constraints), invokes service methods, applies access policies, returns DTO responses with proper status codes and Location headers (on create).
2. **Service Layer** (`service`): Encapsulates business logic, orchestrates repository calls, enforces domain rules (status transitions, ownership checks), triggers auditing.
3. **Repository Layer** (`repository`): Spring Data JPA interfaces that provide CRUD and custom queries (e.g., `AppUserRepository.search`). Abstracts underlying persistence.
4. **Domain Model** (`model`): JPA entities with relationships, constraints, auditing annotations, enums for states and categories.
5. **Security & Context** (`config`, `security`, `util`): Filters (user upsert), JWT decoders, role mapping logic, request-scoped user context, auditing principal resolution.
6. **DTO Layer** (`dto`): Separation between external API surface and internal entities (prevents leaking persistence internals; shapes responses based on consumer needs).

**Typical Request Flow (Authenticated Mutation)**
```
HTTP Request → Security Filter Chain (JWT validation & user upsert) → Controller → UserContextProvider + Access Policy → Service → Repository → Entity Persistence → (Audit Service) → Controller Response DTO
```

**Typical Request Flow (Public Read)**
```
HTTP GET (public endpoint) → Controller (no auth required) → Service (scoped query) → Repository → DTO mapping → Response
```

---
## 4. Key Components
### 4.1 Entry Point
`Msse692BackendApplication` bootstraps Spring and attempts to open Swagger UI automatically (OS-aware launcher). Adds developer convenience.

### 4.2 Security Configuration (`SecurityConfig`)
- Profile-specific beans: separate `SecurityFilterChain` for `dev` vs `local`/`prod`.
- **JWT Validation**:
  - `dev`: issuer only.
  - `local`/`prod`: issuer + audience enforced (custom token validator).
- **Role Resolution**:
  - Prefers `roles` claim from JWT.
  - Fallback to database roles via `AppUserRepository.findByFirebaseUid`.
  - Defaults to `USER` if no roles resolved.
- **Authorization**:
  - Public GET endpoints for health, OpenAPI docs, enum listings, public events feeds, event detail & list.
  - Admin-only endpoints under `/api/admin/users/**`.
  - All other endpoints require auth.
- **Filter Ordering**: `AppUserUpsertFilter` runs after bearer token authentication to ensure database user exists & sets thread-local auditor (`CurrentAuditor`).

### 4.3 User Upsert Filter (`AppUserUpsertFilter`)
- On each authenticated request: checks for existing `AppUser` by Firebase UID.
- Creates record with default `USER` role if absent; updates email/name/photo selectively.
- Prevents overwriting display name when a completed profile exists.
- Sets `CurrentAuditor` for auditing callbacks.

### 4.4 User Context (`UserContextProvider` / `UserContext`)
- Extracts auth principal & authorities to yield `(userId, adminFlag, editorFlag)` record.
- Bridges security layer to business rule evaluation (ownership, modify permissions).

### 4.5 Event Access Policy
- Enforces view/modify rules: ADMIN can view all; EDITOR restricted to own events for modifications; public can view only PUBLISHED.
- Centralizes authorization decisions separate from controllers.

### 4.6 Auditing (`EventAuditServiceImpl`)
- Records create/update/delete actions with actor user id & timestamp (`OffsetDateTime`).
- Actor resolution via current JWT claims → Firebase UID → `AppUser` lookup.
- Provides recent audit trail per event (bounded list).

### 4.7 Persistence Models (Representative)
- `Event`: Core domain entity. Fields: slug, eventName, eventType, startAt/endAt, location, description (Lob), status (enum), auditing fields, version, relationships (Venue, Artists, imageUrls). Uses auditing entity listener.
- `AppUser`: Authentication-correlated user record with roles, Firebase UID, display info.
- `Profile`: Extended user profile (completion status influences name overwrite protection).
- `EventAudit`: Records event modifications.
- Other entities: `Artist`, `Venue`, `RoleRequest` (role elevation workflow), etc.

### 4.8 Repositories (Examples)
- `AppUserRepository`: Lookup by Firebase UID, existence checks, text search with dynamic role filtering.
- `EventRepository`: Extends `JpaRepository` + `JpaSpecificationExecutor` for future flexible filtering (specifications).
- `ProfileRepository`, `RoleRequestRepository`, `EventAuditRepository`: Standard CRUD.

### 4.9 DTOs & Mapping
- DTOs represent externally exposed views (`EventDetailDto`, `EventDto`, `EventAuditDto`, `CreateEventDto`, `UpdateEventDto`).
- MapStruct configured as annotation processor in `maven-compiler-plugin`; potential mapper interfaces (not shown here) convert entities ↔ DTOs with compile-time generation.

### 4.10 Validation
- Bean validation (`jakarta.validation`) ensures incoming request payload fields meet constraints (e.g., `@NotBlank`, `@NotNull`).
- Controller parameters guarded by `@Min`, `@Max`, `@Validated`.

---
## 5. Security Model Summary
| Aspect | Implementation |
|--------|----------------|
| Auth Type | OAuth2 Resource Server (Bearer JWT) |
| Token Source | Firebase issuer URI (profiles vary) |
| Role Claims | `roles` claim → fallback DB roles → default `USER` |
| Authorization | Method-level `@PreAuthorize`, endpoint matchers in `SecurityFilterChain` |
| User Persistence | Upsert on first authenticated request (filter) |
| Auditing Principal | ThreadLocal via `CurrentAuditor` set in filter |
| Profiles | `dev`, `local`, `prod` influence validation strictness |

---
## 6. Event Lifecycle & Status
Typical transitions enforced by service + controller endpoints:
```
DRAFT → PUBLISHED → UNPUBLISHED
        ↘ (Cancellation) CANCELLED
```
- Publish/Unpublish/Cancel endpoints guarded by role (ADMIN/EDITOR) and ownership policy.
- Deletion restricted to ADMIN.

---
## 7. Request Examples (Simplified)
| Action | Endpoint | Method | Auth | Notes |
|--------|----------|--------|------|-------|
| Create Event | `/api/v1/events` | POST | ADMIN/EDITOR | Returns 201 + Location header |
| Get Event | `/api/v1/events/{id}` | GET | Public (PUBLISHED only) / Auth (others) | Policy enforced per status/role |
| List Events | `/api/v1/events` | GET | Public | Pagination, filtering, sort normalization |
| Upcoming Feed | `/api/v1/events/public-upcoming` | GET | Public | Future PUBLISHED only |
| Publish Event | `/api/v1/events/{id}/publish` | POST | ADMIN/EDITOR | Status transition validation |
| Cancel Event | `/api/v1/events/{id}/cancel` | POST | ADMIN/EDITOR | Prevent duplicate cancel |
| My Events | `/api/v1/events/mine` | GET | ADMIN/EDITOR | Owner-scoped listing |
| Event Audits | `/api/v1/events/{id}/audits` | GET | ADMIN/EDITOR (owner) | Returns bounded audit list |

---
## 8. Database & Migrations
- Flyway controls schema via SQL scripts in `classpath:db/migration`.
- `public` schema configured.
- Migrations run at startup (Spring Boot auto integration) or via `flyway-maven-plugin` manually.

---
## 9. Performance & Scaling Considerations
- Paging for list endpoints avoids large result sets.
- Potential future optimization: use Specifications in `EventRepository` for complex dynamic filters.
- Caching layers (e.g., for public upcoming events) can be added at service layer.
- MapStruct provides efficient, compile-time mapping (faster than reflection-based mappers).
- Auditing table (`EventAudit`) bounded by retrieval limits (top 10 / top 50) to reduce load.

---
## 10. Cross-Cutting Concerns
| Concern | Approach |
|---------|---------|
| Logging | SLF4J + per-class loggers (e.g., controller init messages) |
| Error Handling | DTO (`ApiErrorDto`) + validation exceptions map to 400/404/409 |
| Concurrency | Optimistic locking via `@Version` on entities (e.g., `Event.version`) |
| Auditing | Spring Data + custom filter for principal resolution |
| OpenAPI Docs | `springdoc-openapi` auto-scans annotations (`@Operation`, `@ApiResponses`) |
| Thread Context | `CurrentAuditor` handles per-request user id |

---
## 11. Security Extension Points
- **Custom Claims Mapping**: `rolesClaimConverter` can be extended to support additional authorities.
- **Additional Policies**: Implement new policy classes similar to `EventAccessPolicy` for other aggregates.
- **Rate Limiting / Abuse Prevention**: Add filter or use Spring Cloud Gateway external to app.

---
## 12. Testing Strategy (Potential / Recommended)
| Layer | Test Type |
|-------|-----------|
| Repository | Data JPA tests with H2 or Testcontainers PostgreSQL |
| Service | JUnit + mocked repositories (or slicing) |
| Controller | WebMvcTest for request/response validation |
| Security | Spring Security Test (mock JWTs / authorities) |
| Integration | Full stack with Testcontainers, verifying migrations + endpoints |

**Example Integration Flow**: Start PostgreSQL container → run migrations → seed initial data → perform authenticated requests using generated JWTs or mocking decoder.

---
## 13. Future Enhancements
- Introduce domain events for audit instead of direct service calls.
- Add specification-based filtering (date range, status, type) without manual map handling.
- Enhance caching (e.g., Caffeine) for public events feed.
- Implement profile completion workflow & avatar image service integration.
- Add role elevation workflow via `RoleRequest` endpoints + admin approval process.

---
## 14. Operational Notes
| Aspect | Detail |
|--------|--------|
| Health Endpoint | `/actuator/health` (public) |
| Swagger UI | `/swagger-ui.html` auto-open on startup in main class |
| Profiles | `dev`, `local`, `prod` (affect JWT validation strictness) |
| DB Credentials | Provided via environment (Flyway plugin placeholders `flyway.url/user/password`) |
| Port | Default `8080` (override via `server.port`) |

---
## 15. Glossary
| Term | Definition |
|------|------------|
| DTO | Data Transfer Object for API boundary |
| Entity | Persistent domain object managed by JPA |
| AuditorAware | Spring mechanism to populate created/modified user fields |
| JWT | JSON Web Token used for authenticating and conveying user claims |
| MapStruct | Code generator for mapping between Java bean types |
| Flyway | Database migration/versioning tool |

---
## 16. Quick Start Commands
```
# Build
mvn clean package

# Run (dev profile)
mvn spring-boot:run -Pdev

# Run with local profile
mvn spring-boot:run -Plocal

# Flyway migrations only
mvn flyway:migrate -Plocal -Dflyway.url=jdbc:postgresql://localhost:5432/ave -Dflyway.user=... -Dflyway.password=...

# Test
mvn test
```

---
## 17. Diagram (Textual)
```
[Client] → [Controller] → [Service] → [Repository] → [Database]
                  ↓                ↘
            [AccessPolicy]     [AuditService]
                  ↓
           [SecurityContext/JWT] → [AppUserUpsertFilter] → [CurrentAuditor]
```

---
## 18. Maintenance & Conventions
- Keep controllers thin: validation + delegation only.
- Centralize authorization decisions in policy classes.
- Favor constructor injection (immutability of dependencies).
- Use enums for constrained domain values (`EventStatus`, `EventType`).
- MapStruct mappers should remain pure (no side-effects).
- Avoid business logic in entities—keep logic in services.

---
End of backend architecture overview.
