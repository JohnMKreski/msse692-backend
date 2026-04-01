
# Event Times (Community Time: America/Denver)

This backend treats **all event “wall-clock” times as community time in `America/Denver`** (DST-aware). The goal is:

1. Users enter event times as **Denver-local wall time** (e.g., “7:00 PM”).
2. The backend persists times as an **absolute instant** (`Instant`) so comparisons/sorting are reliable.
3. The API returns event times as **Denver-local time with an explicit offset** (`OffsetDateTime`) so clients can display “Denver time” consistently.

> Important terminology: `America/Denver` observes daylight saving time.
> - Summer: **MDT** (UTC-06:00)
> - Winter: **MST** (UTC-07:00)
>
> So the system is “always Denver local time”, not “always -06:00”.

---

## Source of Truth

### Storage (Entity / DB)

- `Event.startAt` and `Event.endAt` are stored as `Instant`.
- This represents an absolute moment in time (effectively UTC), which avoids ambiguity and makes ordering/filtering safe.

Why this matters:
- `LocalDateTime` has no timezone or offset, so it is ambiguous by itself.
- Persisting `Instant` prevents “same wall time, different actual moment” bugs.

### API Request Types (Create/Update)

- `CreateEventDto.startAt/endAt` are `LocalDateTime`.
- `UpdateEventDto.startAt/endAt` are `LocalDateTime`.

These are interpreted as **Denver wall-clock times**.

### API Response Types (List/Detail)

- `EventDto.startAt/endAt` are `OffsetDateTime`.
- `EventDetailDto.startAt/endAt` are `OffsetDateTime`.

These are returned in `America/Denver` with the correct offset (`-06:00` or `-07:00`) for that date.

---

## Conversion Flow

### DTO → Entity (input → stored)

When the API receives a `LocalDateTime` for `startAt/endAt` (create/update), `EventMapper` converts it to an `Instant` using `CommunityTimezone.toInstant(...)`.

This makes a clear promise: **“This LocalDateTime is a Denver-local wall time.”**

### Entity → DTO (stored → output)

When returning event data, `EventMapper` converts stored `Instant` values back into an `OffsetDateTime` using `CommunityTimezone.toDenverOffset(...)`.

This ensures clients see a timestamp like:

```json
"startAt": "2026-04-27T19:00:00-06:00",
"endAt": "2026-04-27T21:00:00-06:00"
```

That example corresponds to **7:00 PM to 9:00 PM Denver time during MDT**.

---

## DST Edge Cases (Spring/Fall)

`CommunityTimezone.toInstant(LocalDateTime)` explicitly handles daylight saving transitions:

- **Spring-forward gap (invalid local times):** throws an `IllegalArgumentException`.
	- Example idea: a local time like `02:30` on the spring-forward date may not exist.
- **Fall-back overlap (ambiguous local times):** chooses the **earlier** offset deterministically.
	- If you ever need users to choose which occurrence they meant, the API would need to accept an offset (e.g., `OffsetDateTime`) at input.

---

## Filtering & “By Date” Queries

When filtering events by date or ranges, the service layer converts the requested wall time into `Instant` using the same community timezone rules (Denver wall time → `Instant`). This keeps querying consistent with storage.

---

## Frontend: Where Display Formatting Should Happen

### Recommendation

**Human-friendly formatting should happen in the frontend**, not the backend.

Backend responsibilities:
- Define the “community timezone” rule (`America/Denver`).
- Store canonical timestamps (`Instant`).
- Return unambiguous timestamps (`OffsetDateTime` with offset).

Frontend responsibilities:
- Format for humans (e.g., `7:00 PM`, `Apr 27, 2026`, `Mon 7–9 PM`).
- Apply the correct display timezone policy.

### Critical frontend note (Denver-time display)

If the product requirement is **“events should display in Denver time for everyone”**, the frontend must *not* rely on the browser’s default timezone.

In practice, format using an explicit timezone (IANA name): `America/Denver`.

If the frontend instead formats with the user’s local timezone, a user in a different timezone will see a shifted time.

---

## Summary

- Input (create/update): `LocalDateTime` is interpreted as **Denver wall time**.
- Storage: `Instant` is the **canonical** persisted representation.
- Output (API responses): `OffsetDateTime` in **America/Denver** with explicit offset.
- Display formatting: **frontend** (using Denver timezone when required).

