The Artist model uses a @ManyToMany relationship with Event, meaning an artist can perform at many events and an event can feature many artists. For a Venue model, the relationship is typically @OneToMany with Event, since a venue hosts many events, but each event usually occurs at one venue. So, in the Venue model, you would use:

We have three models: Artist, Event, and Venue.

- **Artist ↔ Event**: Uses a `@ManyToMany` relationship. An artist can perform at many events, and an event can feature many artists.
- **Venue ↔ Event**: Uses a `@OneToMany` relationship from Venue to Event. A venue hosts many events, but each event usually occurs at one venue (`@ManyToOne` from Event to Venue).

**Difference**:
- `@ManyToMany` allows both sides to have multiple references to each other.
- `@OneToMany`/`@ManyToOne` means one side (Venue) has many of the other (Event), but each Event has only one Venue.

Further considerations for a CMS
To turn these core entities into a full CMS, you would build on this foundation with additional Spring components:

    Repositories: Create Spring Data JPA repositories (e.g., ConcertEventRepository extends JpaRepository<ConcertEvent, Long>) to automatically provide methods for CRUD (Create, Read, Update, Delete) operations.
    Services: A service layer (e.g., ConcertEventService) would encapsulate business logic, like validating event data or managing relationships.
    Controllers: Implement Spring MVC REST controllers to handle HTTP requests for managing the CMS content. An admin-facing API could expose endpoints for creating, updating, and deleting events, venues, and artists.
    Security: Use Spring Security to secure CMS endpoints, ensuring only authenticated and authorized administrators can perform administrative tasks.
    UI/Frontend: A templating engine like Thymeleaf, integrated with Spring MVC, can render the web pages for both the public-facing and admin interfaces. For a modern approach, the controllers could expose REST APIs, and a separate JavaScript-based frontend could consume them. 

# Models Design Guide

This document explains the role of **Models (Entities)** in the Ark Valley Events backend. Models define the database structure and act as the foundation of the persistence layer. This guide will be updated as we refine their design.

---

## Why Models?

* **Entities** represent the **domain objects** (Event, Artist, Venue).
* They are mapped to database tables using **JPA/Hibernate annotations** (`@Entity`, `@Id`, `@ManyToMany`, etc.).
* Entities enforce **data integrity rules** at the persistence level (e.g., a `Venue` must always have a name).
* They manage **relationships** between domain objects (Event ↔ Artist, Event ↔ Venue).

---

## Role of Models vs. DTOs

* **Models (Entities):**

    * Define what the **database looks like**.
    * Capture **true business invariants** (e.g., no Event without a date).
    * Are used internally by the backend for persistence and queries.

* **DTOs (Data Transfer Objects):**

    * Define what the **API looks like**.
    * Separate request validation and response shaping from persistence.
    * Prevent recursion issues and hide internal details from the frontend.

---

## Current Models

The Ark Valley Events backend currently defines three models:

* `Event` — represents a live event, with fields such as name, date, time, and description.
* `Artist` — represents a performer or group, with fields such as name, genre, biography, and website.
* `Venue` — represents a location that hosts events, with fields such as name, address, capacity, and description.

These models are connected:

* An `Event` can have multiple `Artists` (many-to-many).
* An `Event` belongs to a `Venue` (many-to-one).
* A `Venue` can host many `Events` (one-to-many).

---

## Summary

* Models = **database layer** → enforce persistence rules.
* DTOs = **API layer** → enforce API contracts.
* Entities ensure the **data in the database is valid**.
* Request DTOs ensure **client input is valid** before it becomes an entity.
* Response DTOs ensure **frontend receives clean, structured data**.

This separation allows flexibility: we can evolve the database schema or the API independently without breaking each other.
