# DTO Design Guide

This document explains the DTO (Data Transfer Object) structure and conventions used in the Ark Valley Events backend. DTOs define how data flows between the backend and the frontend, separating **database entities** from **API contracts**.

---

## Why DTOs?

* **Entities**: Represent the database schema (persisted via JPA/Hibernate).
* **DTOs**: Represent the data exchanged in the API layer.
* Prevents exposing internal database structures directly to the client.
* Avoids infinite recursion in bidirectional relationships (e.g., Event ↔ Artist).
* Makes the API responses predictable, safe, and easier to evolve.

---

## Package Structure

```
com.arkvalleyevents.msse692_backend.dto
│
├── request   // DTOs for data coming *into* the backend (client → API)
│   ├── CreateEventDto.java
│   ├── UpdateEventDto.java
│   ├── CreateArtistDto.java
│   ├── UpdateArtistDto.java
│   ├── CreateVenueDto.java
│   └── UpdateVenueDto.java
│
└── response  // DTOs for data going *out* of the backend (API → client)
    ├── EventDto.java
    ├── EventDetailDto.java
    ├── ArtistDto.java
    ├── ArtistDetailDto.java
    ├── VenueDto.java
    └── VenueDetailDto.java
```

---

## Request DTOs

Request DTOs are used for **input validation**. They define what fields a client must send when creating or updating records.

* `CreateXDto` → used for POST (creating new records). Required fields are annotated with `@NotNull`, `@NotBlank`, etc.
* `UpdateXDto` → used for PUT/PATCH (updating records). Fields are optional to allow partial updates.

**Examples:**

* `CreateEventDto` includes `venueId` and `artistIds` (references by ID, not nested objects).
* `CreateArtistDto` and `CreateVenueDto` enforce names/addresses as required fields.

---

## Response DTOs

Response DTOs are used to send structured, safe data back to the frontend.

* `XDto` → lightweight summary version (e.g., event list cards).
* `XDetailDto` → richer version, used for detail views (`/events/{id}`, `/venues/{id}`, etc.).

**Examples:**

* `EventDto` includes `venueName` and `artistNames` (flattened strings for quick listings).
* `EventDetailDto` nests `VenueDto` and `ArtistDto` for detailed drill-down views.
* `ArtistDetailDto` and `VenueDetailDto` include lists of related `EventDto`s instead of deep nested objects (prevents recursion).

---

## Summary

* **Entities** = database layer
* **DTOs** = API layer (separated into request + response)
* **Request DTOs** = validate input (create/update)
* **Response DTOs** = shape output (summary/detail)
* **Flatten relationships where possible**, only use nested detail DTOs when the frontend needs them

This approach keeps the backend clean, prevents recursion issues, and provides a consistent API contract for the frontend.
