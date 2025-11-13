# Ark Valley Events API (v1) — Endpoint Reference

This document describes the backend HTTP API exposed by msse692-backend.
It covers endpoint purpose, access rules, inputs, outputs, examples, and notes.

- Base path: `/api` (versioned paths under `/api/v1` unless noted)
- Auth: Bearer JWT (Firebase) when required
- Roles: `ADMIN`, `EDITOR`, `USER` (anonymous has no role)
- Time policy (current):
  - Create/Update request uses `Instant` for `startAt`/`endAt` (e.g., `2025-11-20T02:00:00Z`).
  - These map to server-local `LocalDateTime` for storage/display (server zone).
  - Responses expose event `startAt`/`endAt` as `LocalDateTime` (no zone offset), and audit fields as `Instant`/`OffsetDateTime`.
- Pagination and sorting (events list): `page` (0-based, min 0), `size` (min 1, max 100), `sort` (whitelist: `startAt`, `eventName`; formats: `field`, `field,desc`, `-field`).
- List response shape: `{ items: EventDto[], page: { number, size, totalElements, totalPages } }`.
- Error shape: `{ timestamp, status, error, code, message, path, requestId, details? }` via global handler.

Tip: Public GETs work without Authorization. If you attach `Authorization: Bearer <token>`, some results change per role rules.

---

## Events

Base: `/api/v1/events`

### Create Event
- Method/Path: `POST /api/v1/events`
- Access: `ADMIN`, `EDITOR`
- Body (CreateEventDto):
  ```json
  {
    "eventName": "Summer Jam",
    "type": "CONCERT",
    "startAt": "2025-11-20T02:00:00Z",
    "endAt": "2025-11-20T04:00:00Z",
    "eventLocation": "Salida, CO",
    "eventDescription": "Live music"
  }
  ```
- Responses:
  - 201 Created + `Location: /api/v1/events/{id}`
  - Body: EventDetailDto
- Notes: Slug generated from `eventName`; status defaults to `DRAFT`.

### Get Event (Detail)
- Method/Path: `GET /api/v1/events/{id}`
- Access/Visibility:
  - `ADMIN`: any event
  - `EDITOR`: only events created by the caller
  - Anonymous/`USER`: only `PUBLISHED` events (non-public return 404)
- Responses: 200 EventDetailDto or 404
- Notes: Consider adding ETag and `Vary: Authorization`.

### Update Event
- Method/Path: `PUT /api/v1/events/{id}`
- Access: `ADMIN`, `EDITOR`
- Body (UpdateEventDto, partial; nulls ignored):
  ```json
  {
    "eventName": "Summer Jam (Updated)",
    "type": "CONCERT",
    "startAt": "2025-11-20T02:00:00Z",
    "endAt": "2025-11-20T04:00:00Z",
    "eventLocation": "Salida, CO",
    "eventDescription": "Live music and food"
  }
  ```
- Responses: 200 EventDetailDto or 404
- Notes: Validate `startAt < endAt`.

### Delete Event
- Method/Path: `DELETE /api/v1/events/{id}`
- Access: `ADMIN`
- Responses: 204 No Content or 404

### Publish Event
- Method/Path: `POST /api/v1/events/{id}/publish`
- Access: `ADMIN`, `EDITOR`
- Responses:
  - 200 EventDetailDto (status becomes `PUBLISHED`)
  - 409 on illegal state (only `DRAFT` can be published)

### Unpublish Event
- Method/Path: `POST /api/v1/events/{id}/unpublish`
- Access: `ADMIN`, `EDITOR`
- Responses:
  - 200 EventDetailDto (status becomes `UNPUBLISHED`)
  - 409 on illegal state (only `PUBLISHED` can be unpublished)

### Cancel Event
- Method/Path: `POST /api/v1/events/{id}/cancel`
- Access: `ADMIN`, `EDITOR`
- Responses:
  - 200 EventDetailDto (status becomes `CANCELLED`)
  - 409 if already cancelled

### List Events
- Method/Path: `GET /api/v1/events`
- Access/Defaults:
  - `ADMIN`: no default filter
  - `EDITOR`: defaults to `createdByUserId=<me>` and, if no `status`, sets `ownerOrPublished=true` (own items OR all PUBLISHED)
  - Anonymous/`USER`: defaults to `status=PUBLISHED`
- Query params:
  - Paging: `page` (min 0), `size` (min 1, max 100)
  - Sorting: `sort` (allowed: `startAt`, `eventName`; examples: `startAt,asc`, `-startAt`)
  - Filters (strings):
    - `status` (EventStatus enum)
    - `createdByUserId` (long)
    - `ownerOrPublished` (boolean; special OR behavior when true and no `status`)
    - `from` / `to` (ISO `LocalDateTime`, filter by `startAt` range)
- Responses:
  - 200 EventPageResponse
    ```json
    {
      "items": [
        {
          "eventId": 42,
          "eventName": "Summer Jam",
          "type": "CONCERT",
          "startAt": "2025-06-01T18:00:00",
          "endAt": "2025-06-01T20:00:00",
          "eventLocation": "Salida, CO",
          "status": "PUBLISHED",
          "slug": "summer-jam"
        }
      ],
      "page": {
        "number": 0,
        "size": 20,
        "totalElements": 153,
        "totalPages": 8
      }
    }
    ```
  - 400 ApiErrorDto on invalid params (e.g., size out of range, unsupported sort)
- Notes:
  - Add `Vary: Authorization` (results differ when authenticated).

### Public Upcoming (feed)
- Method/Path: `GET /api/v1/events/public-upcoming`
- Access: Public
- Query params:
  - `from`: Instant; default now; used to compute UTC LocalDateTime bound
  - `limit`: int; clamped 1..100; default 10
- Responses: 200 List<EventDto> (only future `PUBLISHED` events, ascending by `startAt`)
- Notes: Consider short `Cache-Control` and optional filter by `type`.

### Event Audits (read-only trail)
- Method/Path: `GET /api/v1/events/{id}/audits`
- Access: Currently open; consider restricting to `ADMIN` or owning `EDITOR`.
- Query params: `limit` (default 10)
- Responses: 200 List<EventAuditDto)

---

## Enums

Base: `/api/v1/enums`

### Event Types
- `GET /api/v1/enums/event-types`
- Access: Public
- Responses: 200 `[{ value, label }]`
- Notes: Cache-Control public, max-age=6h

### Event Statuses
- `GET /api/v1/enums/event-statuses`
- Access: Public
- Responses: 200 `[{ value, label }]`
- Notes: Cache-Control public, max-age=6h

---

## Profile

Base: `/api/v1/profile`

### Get My Profile
- `GET /api/v1/profile/me`
- Access: Auth required
- Responses:
  - 200 ProfileResponse
  - 404 `{ code: "PROFILE_NOT_FOUND", ... }`

### Upsert My Profile
- `POST /api/v1/profile`
- Access: Auth required
- Body (ProfileRequest):
  ```json
  { "displayName": "Jane Doe" }
  ```
- Responses:
  - 201 Created (new)
  - 200 OK (updated)

---

## Auth (utility)

Base: `/api/v1/auth`

### Who Am I
- `GET /api/v1/auth/whoami`
- Access: Public (returns `authenticated=false` if no token)
- Responses: 200 `{ authenticated, name?, subject?, claims?, authorities }`
- Notes: Debug endpoint; consider dev-only in prod.

---

## App Users

Base: `/api/v1/app-users`

### Me (AppUser)
- `GET /api/v1/app-users/me`
- Access: Auth required
- Responses:
  - 200 AppUser entity
  - 404 `{ code: "APP_USER_NOT_FOUND", ... }`
- Notes: Consider switching to a DTO response.

---

## Admin — Roles

Base: `/api/admin/users` (not versioned)

### Get Roles
- `GET /api/admin/users/{uid}/roles`
- Access: `ADMIN`
- Responses: 200 `{ firebaseUid, roles: ["USER", "EDITOR", ...] }`

### Add Roles
- `POST /api/admin/users/{uid}/roles`
- Access: `ADMIN`
- Body: `{ "roles": ["EDITOR", "ADMIN"] }`
- Responses: 200 RolesResponse; 400 on unknown roles
- Notes: Roles validated to {USER, EDITOR, ADMIN}; action audited in logs; triggers claims sync.

### Remove Role
- `DELETE /api/admin/users/{uid}/roles/{role}`
- Access: `ADMIN`
- Responses: 200 with details, or 304 Not Modified if role absent

### Sync Claims
- `POST /api/admin/users/{uid}/roles/sync?force=false`
- Access: `ADMIN`
- Responses: 202 Accepted with operation details

---

## Examples (curl)

Create event (EDITOR):
```bash
curl -X POST "${API}/api/v1/events" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "Summer Jam",
    "type": "CONCERT",
    "startAt": "2025-11-20T02:00:00Z",
    "endAt": "2025-11-20T04:00:00Z",
    "eventLocation": "Salida, CO"
  }'
```

List events (anonymous → published only):
```bash
curl "${API}/api/v1/events?page=0&size=20&sort=startAt,asc"
```

List events (EDITOR → own + published):
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "${API}/api/v1/events?page=0&size=20&sort=startAt,asc"
```

Public upcoming:
```bash
curl "${API}/api/v1/events/public-upcoming?from=2025-11-10T00:00:00Z&limit=10"
```

---

## Notes and Future Improvements
- Optionally add pagination `Link` headers and `X-Total-Count` for broader client compatibility.
- Add `Vary: Authorization`, ETag, and cache headers where appropriate.
- Tighten visibility on `/audits` endpoint if needed.
