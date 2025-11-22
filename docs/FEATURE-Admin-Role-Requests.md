# Admin Role Requests (Backend)

This document describes the backend API for the Admin Role Requests workflow, including domain model, authentication/authorization, endpoints, parameters, and example requests/responses.

## Overview

Users can submit a role request (e.g., EDITOR). Admins can list, inspect, approve, or reject those requests. The API is exposed under the non-versioned path prefix `/api`.

- User endpoints: `/api/roles/requests[...]`
- Admin endpoints: `/api/admin/users/roles/requests[...]`
- Authentication: Firebase JWT (attached by frontend); backend maps to application roles.
- Authorization: Admin-only access to `/api/admin/**`; users can only view/manipulate their own requests.

## Domain Model

`RoleRequest` (serialized to JSON)
- `id: string`
- `requesterUid: string`
- `requestedRoles: string[]` (e.g., `["EDITOR"]`)
- `status: string` — display label (e.g., `"Pending"`, `"Approved"`, `"Rejected"`, `"Canceled"`)
- `approverUid?: string | null`
- `reason?: string | null`
- `approverNote?: string | null`
- `createdAt?: string` (ISO-8601)
- `updatedAt?: string` (ISO-8601)
- `decidedAt?: string` (ISO-8601)

Notes
- New requests default to status `PENDING` and serialize as `"Pending"`.
- Status JSON uses user-friendly display names.
- `decidedAt` is set when a request is approved/rejected.

## Pagination Wrapper

All list endpoints return a Spring-style page:
- `content: T[]`
- `number: number` (0-based page)
- `size: number`
- `totalElements: number`
- `totalPages: number`
- `first: boolean`, `last: boolean`

## Authentication & Authorization

- Auth: Firebase JWT (Bearer token).
- User routes require an authenticated user; actions are restricted to the caller's own requests.
- Admin routes require an authenticated user with ADMIN role.
- Typical errors: `401 Unauthorized` (missing/invalid token), `403 Forbidden` (insufficient role).

## User Endpoints

### Create request
POST `/api/roles/requests`

Body
```json
{
  "requestedRoles": ["EDITOR"],
  "reason": "I will curate event content."
}
```

Response `200 OK`
```json
{
  "id": "a1b2c3",
  "requesterUid": "uid_123",
  "requestedRoles": ["EDITOR"],
  "status": "Pending",
  "reason": "I will curate event content.",
  "createdAt": "2025-11-17T20:10:45Z",
  "updatedAt": "2025-11-17T20:10:45Z"
}
```

### List my requests
GET `/api/roles/requests`

Query params
- `page`: number (0-based)
- `size`: number
- `sort`: string, e.g. `createdAt,desc`
- `status`: repeated or single; enum name(s) expected (e.g., `PENDING`, `APPROVED`, `REJECTED`, `CANCELED`)

Response `200 OK`
```json
{
  "content": [
    {
      "id": "a1b2c3",
      "requesterUid": "uid_123",
      "requestedRoles": ["EDITOR"],
      "status": "Pending",
      "createdAt": "2025-11-17T20:10:45Z",
      "updatedAt": "2025-11-17T20:10:45Z"
    }
  ],
  "number": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### Cancel my pending request
POST `/api/roles/requests/{id}/cancel`

Response `200 OK`
```json
{
  "id": "a1b2c3",
  "status": "Canceled",
  "updatedAt": "2025-11-17T21:05:12Z"
}
```

Notes
- Only `Pending` requests can be canceled.
- Cancelling sets status to `Canceled` and updates timestamps.

## Admin Endpoints

### List role requests
GET `/api/admin/users/roles/requests`

Query params
- `page`: number (0-based)
- `size`: number
- `sort`: string, e.g., `createdAt,desc` or `updatedAt,asc`
- `status`: repeated `PENDING|APPROVED|REJECTED|CANCELED`
- `q`: optional free-text search (UID/email/etc.), if supported

Response `200 OK`
```json
{
  "content": [
    {
      "id": "a1b2c3",
      "requesterUid": "uid_123",
      "requestedRoles": ["EDITOR"],
      "status": "Pending",
      "createdAt": "2025-11-17T20:10:45Z"
    }
  ],
  "number": 0,
  "size": 20,
  "totalElements": 37,
  "totalPages": 2,
  "first": true,
  "last": false
}
```

### Get request by ID
GET `/api/admin/users/roles/requests/{id}`

Response `200 OK`
```json
{
  "id": "a1b2c3",
  "requesterUid": "uid_123",
  "requestedRoles": ["EDITOR"],
  "status": "Pending",
  "reason": "I will curate event content.",
  "createdAt": "2025-11-17T20:10:45Z",
  "updatedAt": "2025-11-17T20:10:45Z"
}
```

### Approve request
POST `/api/admin/users/roles/requests/{id}/approve`

Body
```json
{ "approverNote": "Welcome aboard." }
```

Response `200 OK`
```json
{
  "id": "a1b2c3",
  "status": "Approved",
  "approverUid": "admin_789",
  "approverNote": "Welcome aboard.",
  "decidedAt": "2025-11-17T21:15:03Z",
  "updatedAt": "2025-11-17T21:15:03Z"
}
```

### Reject request
POST `/api/admin/users/roles/requests/{id}/reject`

Body
```json
{ "approverNote": "Insufficient justification." }
```

Response `200 OK`
```json
{
  "id": "a1b2c3",
  "status": "Rejected",
  "approverUid": "admin_789",
  "approverNote": "Insufficient justification.",
  "decidedAt": "2025-11-17T21:20:44Z",
  "updatedAt": "2025-11-17T21:20:44Z"
}
```

Notes
- Only `Pending` requests can be approved or rejected.
- Approvals and rejections set `decidedAt`, `updatedAt`, and the `approverUid` of the acting admin.

## Status Handling

- Incoming filter values (`status` query param) should use enum names: `PENDING`, `APPROVED`, `REJECTED`, `CANCELED`.
- Outgoing JSON responses render the display names: `"Pending"`, `"Approved"`, `"Rejected"`, `"Canceled"`.

## Errors

Standard error shape (`ApiErrorDto`):
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Admin role required",
  "path": "/api/admin/users/roles/requests"
}
```

## cURL Examples

List pending requests (admin):
```bash
curl -H "Authorization: Bearer <TOKEN>" \
  "https://<host>/api/admin/users/roles/requests?status=PENDING&sort=createdAt,desc&page=0&size=20"
```

Approve a request:
```bash
curl -X POST -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"approverNote":"Welcome aboard."}' \
  "https://<host>/api/admin/users/roles/requests/a1b2c3/approve"
```

Cancel my request (user):
```bash
curl -X POST -H "Authorization: Bearer <TOKEN>" \
  "https://<host>/api/roles/requests/a1b2c3/cancel"
```

## Implementation Notes

- Controller mappings live under `/api` (not `/api/v1`).
- `@JsonValue` (or equivalent) ensures display names for status on serialization.
- Default status for new entities is `PENDING`.
- Sorting typically supports `createdAt` and `updatedAt`.
