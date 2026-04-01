# Venue future implementation

This document captures the current state of the backend Venue infrastructure, why the database schema may differ from Flyway migrations, and a concrete plan to implement venue pages and typo-proof venue selection.

## Current state (what exists)

### JPA model
- `Venue` entity exists with:
  - `venueId`, `name`, `address`, `capacity`, `description`, `website`
  - `@OneToMany(mappedBy = "venue")` set of events
- `Event` entity has:
  - `@ManyToOne(fetch = LAZY)` to `Venue`
  - `@JoinColumn(name = "venue_id")`
  - also still has `eventLocation` (string) for events without a venue

### DTOs
- `VenueDto` and `VenueDetailDto` exist.
- `VenueDetailDto` is explicitly intended for a venue profile page:
  - Comment indicates: `GET /venues/{id}`
  - Includes `Set<EventDto> events`

### Mapping / wiring status
- Event mapping currently ignores venue:
  - `EventMapper` has `@Mapping(target = "venue", ignore = true)` for create/update.
  - Event DTOs contain venue-related fields commented out (not exposed in API).

### Missing components (not found in repo)
- No `VenueRepository`, `VenueService`, or `VenueController` classes were found.
- No endpoints/routes for `/venues` were found (other than a comment in `VenueDetailDto`).

## Why the DB schema may not match migrations

Flyway migrations in `src/main/resources/db/migration` do **not** include:
- `CREATE TABLE venue ...`
- `event.venue_id` foreign key column

However, application configuration can still create/update schema via Hibernate.

### Profiles and schema behavior
- **dev** (H2):
  - `spring.jpa.hibernate.ddl-auto: update`
  - `spring.flyway.enabled: false`
  - Effect: Hibernate can auto-create tables/columns from entities (including `venue` and `event.venue_id`).

- **test** (H2):
  - Same pattern: `ddl-auto: update`, Flyway disabled.

- **prod** (PostgreSQL):
  - Flyway enabled, but `hibernate.ddl-auto: update` is also enabled.
  - Effect: schema may drift because Hibernate can add/alter tables outside of migrations.

- **local** (PostgreSQL):
  - Flyway enabled, `hibernate.ddl-auto: validate`
  - `baseline-on-migrate: true`, `baseline-version: 1`
  - Effect: expects an existing schema; Flyway treats the current schema as already baseline-applied.

## Problem statement (what we want)

1) **Dynamic venue pages**
- A venue page should show:
  - venue main details (name/address/website/etc.)
  - events hosted at the venue

2) **Typos prevented in event creation/editing**
- Avoid free-form venue/location strings where appropriate.
- Prefer selecting an existing venue (controlled list), but still allow non-venue locations when needed.

## Recommended implementation plan

### Phase 0 — Decide the product rules (minimal decisions)

- Should an event always have either:
  - `venueId` (reference to Venue), OR
  - `eventLocation` (free text), OR
  - both?

Suggested rule:
- Use `venueId` when it’s a known venue.
- Keep `eventLocation` for non-venue or “custom” locations.

### Phase 1 — Make schema ownership consistent

Goal: ensure the schema is reproducible across environments.

- Add Flyway migrations to create:
  - `venue` table
  - `event.venue_id` column and FK constraint

- Prefer setting in **prod**:
  - `spring.jpa.hibernate.ddl-auto: validate`
  - (so Flyway is the single schema owner)

Notes:
- If prod already has a venue table created by Hibernate, handle it with baseline/repair strategy rather than dropping data.

### Phase 2 — Add persistence layer for venues

- `VenueRepository extends JpaRepository<Venue, Long>`
- Consider a `findByNameIgnoreCase` or `findBySlug` depending on URL strategy.

### Phase 3 — Add read-only Venue API (first)

Minimum endpoints for venue pages:
- `GET /venues`
  - returns list of venues (id + name + address; maybe website)
- `GET /venues/{venueId}`
  - returns venue details

For “venue page shows events” there are two viable approaches:

**Option A (embedded events in venue detail)**
- `GET /venues/{id}` returns `VenueDetailDto` with `events`.
- Requires mapping `Venue -> VenueDetailDto` and mapping `Event -> EventDto`.
- Watch out for LAZY loading; likely need a fetch-join query or explicit loading.

**Option B (separate events query)**
- `GET /venues/{id}` returns venue only.
- `GET /events?venueId={id}` returns events for that venue.
- Often cleaner; keeps Venue DTOs stable and avoids large payloads.

### Phase 4 — Wire venue into event create/update (optional, after read-only API)

- Un-comment/add `venueId` in:
  - `CreateEventDto`, `UpdateEventDto`
  - `EventDto` and `EventDetailDto` (either `venueId` only or a nested `VenueDto`)

- In service layer, resolve venue:
  - if `venueId` provided, load it and set `event.setVenue(venue)`

- Update MapStruct mappings accordingly:
  - Keep `venue` ignored in mapper if resolution is done in service.

### Phase 5 — Frontend integration notes (future)

- Event editor:
  - Replace free-form venue selection with a typeahead/select backed by `GET /venues`.
  - Keep a “Custom location” fallback for non-venue events.

- Venue page:
  - Route like `/venues/:id` (or slug later).
  - Display venue details + list of events.

## Key risks / gotchas

- **Schema drift**: leaving `ddl-auto=update` on in prod can silently change schema without migrations.
- **Serialization pitfalls**:
  - Bidirectional relationships (`Venue.events` and `Event.venue`) can cause recursion if entities are serialized directly.
  - Prefer DTOs and explicit mapping (which you already do).
- **LAZY loading**:
  - Returning events inside venue detail may require fetch joins to avoid `LazyInitializationException`.
- **Data consistency**:
  - If you keep both `venueId` and `eventLocation`, define which is authoritative for display.

## Open questions to answer before coding

1) Venue page URL:
- numeric id (`/venues/123`) vs slug (`/venues/sundry`)

2) Event display:
- Should event detail show venue name/address (from venue) when venue is present?

3) Admin workflow:
- Who can create/edit venues (admin only)?
- Is venue data curated or user-generated?
