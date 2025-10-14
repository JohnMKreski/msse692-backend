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


