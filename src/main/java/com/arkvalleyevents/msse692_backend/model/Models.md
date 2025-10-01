The Artist model uses a @ManyToMany relationship with Event, meaning an artist can perform at many events and an event can feature many artists. For a Venue model, the relationship is typically @OneToMany with Event, since a venue hosts many events, but each event usually occurs at one venue. So, in the Venue model, you would use:

We have three models: Artist, Event, and Venue.

- **Artist ↔ Event**: Uses a `@ManyToMany` relationship. An artist can perform at many events, and an event can feature many artists.
- **Venue ↔ Event**: Uses a `@OneToMany` relationship from Venue to Event. A venue hosts many events, but each event usually occurs at one venue (`@ManyToOne` from Event to Venue).

**Difference**:
- `@ManyToMany` allows both sides to have multiple references to each other.
- `@OneToMany`/`@ManyToOne` means one side (Venue) has many of the other (Event), but each Event has only one Venue.