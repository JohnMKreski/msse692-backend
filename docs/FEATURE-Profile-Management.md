# Profile Management Feature

This document outlines the backend profile feature: data model, validation rules, API endpoints, mapper behavior, and the schema migration that enables it.

## Overview
Profiles represent a user’s public presence. Supported types: `VENUE`, `ARTIST`, `OTHER`. A profile belongs 1:1 to a user and can include display name, type, optional location and description, plus arrays of socials and websites.

## Data Model
Table: `profiles`
- `id`: BIGSERIAL PK
- `user_id`: BIGINT, unique, FK -> `app_users(id)` (cascade delete)
- `display_name`: VARCHAR(200), required
- `profile_type`: VARCHAR(16), required; values: `VENUE|ARTIST|OTHER`
- `location`: VARCHAR(255), optional (required for `VENUE`)
- `description`: TEXT, optional
- `socials`: JSONB array of strings (stored as JSON), default `[]`
- `websites`: JSONB array of strings (stored as JSON), default `[]`
- `completed`: BOOLEAN (derived from `display_name`)
- `verified`: BOOLEAN
- `created_at`, `updated_at`: TIMESTAMPTZ (managed by entity callbacks)

Entity: `com.arkvalleyevents.msse692_backend.model.Profile`
DTOs:
- Request: `dto.request.ProfileRequest`
- Response: `dto.response.ProfileResponse`

## Migration (V6__extend_profile_fields.sql)
- Adds columns: `profile_type`, `location`, `description`, `socials`, `websites`.
- Backfills `profile_type` to `OTHER` for existing rows and drops default.
- Adds check constraint `profiles_profile_type_chk` for allowed types.
- Adds unique index `profiles_user_id_uniq`.
- Adds optional partial index `profiles_venue_location_idx` for venue lookups.

## Validation & Business Rules
- `displayName`: required; max 200; trimmed on save.
- `profileType`: required.
- `location`:
  - Free-form string up to 255 characters (addresses are fine).
  - Required when `profileType == VENUE`; value is trimmed.
- `socials`/`websites`: arrays of strings via JSON; application stores as JSONB.
- `completed`: true if `displayName` is non-empty (set in service).

Service enforcement: `ProfileServiceImpl.enforceVenueLocationRule` throws `IllegalArgumentException("LOCATION_REQUIRED_FOR_VENUE")` if venue has missing/blank location.

## Mapper Behavior
Mapper: `service.mapping.ProfileMapper`
- Request -> Entity: converts `socials`/`websites` lists to JSON string.
- Entity -> Response: converts stored JSON to lists.
- Patch semantics: `updateEntity` ignores `null` fields; `socials`/`websites` are only updated when provided (preserve existing values on partial updates).

## API Endpoints
Base paths (representative, consult `ProfileController` for exact mappings):

User (authenticated):
- `GET /api/profiles/me` → current user’s profile
- `POST /api/profiles` (upsert; legacy/deprecated usage)
- `POST /api/profiles/create` → create if absent (409 if exists)
- `PUT /api/profiles` → full update (replace semantics)
- `PATCH /api/profiles` → partial update (nulls ignored)
- `DELETE /api/profiles` → delete current profile

Admin (ROLE_ADMIN):
- `GET /api/admin/users/{userId}/profile`
- `POST /api/admin/users/{userId}/profile`
- `PUT /api/admin/users/{userId}/profile`
- `PATCH /api/admin/users/{userId}/profile`
- `DELETE /api/admin/users/{userId}/profile`

### Sample Requests
Create (current user):
```http
POST /api/profiles/create
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "displayName": "The Ark Valley Hall",
  "profileType": "VENUE",
  "location": "123 Main St, Salida, CO 81201",
  "description": "Historic event hall.",
  "socials": ["https://instagram.com/arkvalleyevents"],
  "websites": ["https://arkvalleyevents.com"]
}
```

Patch (current user):
```http
PATCH /api/profiles
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "description": "Now with outdoor patio.",
  "socials": ["https://instagram.com/arkvalleyevents", null]
}
```
Notes: null entries in arrays are tolerated by the mapper tests; null fields in the payload are ignored (preserve existing values) except when explicitly provided for lists.

## Errors
- `USER_NOT_FOUND`
- `PROFILE_ALREADY_EXISTS`
- `PROFILE_NOT_FOUND`
- `LOCATION_REQUIRED_FOR_VENUE`

## Security
- User routes require a valid JWT.
- Admin routes require `ROLE_ADMIN`.

## Testing Summary
- Mapper tests validate list<->JSON conversion and partial update semantics.
- Service tests cover create/update/patch/delete for user and admin flows, venue rule enforcement, and error cases.
- Controller tests validate endpoints, auth constraints, and validation payloads. Mapper is mocked in slice tests.

## Frontend Considerations
- `ProfileRequest` must include `displayName` and `profileType`.
- For `VENUE`, ensure `location` is provided and ≤ 255 chars.
- `socials`/`websites` are arrays of strings; nulls inside arrays are tolerated but should be avoided in UI.
