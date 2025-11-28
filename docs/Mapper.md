# Mapper Usage in msse692-backend

This backend uses MapStruct mappers as Spring beans to convert between DTOs and JPA entities. Mappers are intentionally free of business logic and persistence concerns; services orchestrate creation, updates, and relationships while invoking mappers for data transformation.

## Principles
- Component model: `@Mapper(componentModel = "spring")` to allow DI.
- Separation of concerns: mappers do mapping only; services set IDs, timestamps, slugs, statuses, relations.
- Partial updates: `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` to treat nulls as "do not overwrite".
- Drift safety: `unmappedTargetPolicy = IGNORE` (can tighten to `ERROR` as needed).
- Type conversions: small helper methods where necessary (e.g., JSON strings ↔ lists).

## EventMapper
- Methods:
  - `toEntity(CreateEventDto)`: DTO → `Event` for creation; ignores service-controlled fields (`eventId`, `status`, `slug`, timestamps, relations).
  - `updateEntity(Event, UpdateEventDto)`: PATCH-like merge into an existing entity; ignores nulls and service-controlled fields.
  - `toDto(Event)` / `toDetailDto(Event)`: Entity → DTOs; maps enum display names.
- Relations (venue/artists) are not mapped here; services handle association and may use additional mappers via `uses = { ... }`.

## ProfileMapper
- Methods:
  - `toEntity(ProfileRequest)`: DTO → `Profile` for creation; ignores service-controlled fields (`id`, `user`, `completed`, `verified`, timestamps). Serializes `socials`/`websites` lists to JSON array strings stored in the entity.
  - `updateEntity(Profile, ProfileRequest)`: Partial merge (PATCH-like); nulls do not overwrite; same ignores as creation. Serializes list fields to JSON.
  - `toResponse(Profile)`: Entity → `ProfileResponse`; deserializes JSON strings back to `List<String>` and exposes `userId` if present.
- Services enforce validation (e.g., `VENUE` requires `location`) and uniqueness per user; mappers do not validate or query.

## How Services Use Mappers
- Create flow:
  1. Validate request (bean validation + domain rules).
  2. `mapper.toEntity(request)` to get a new entity.
  3. Set service-managed fields (e.g., `user`, `status`, relations).
  4. Persist via repository.
  5. `mapper.toDto(entity)` or `mapper.toResponse(entity)` for return.
- Update flow:
  1. Load existing entity by ID/user.
  2. Validate allowed changes.
  3. `mapper.updateEntity(entity, requestDto)` to apply partial updates.
  4. Persist.
  5. Map to outbound DTO.

## Testing Guidance
- Unit tests: verify field mappings and null-ignore behavior.
- Integration tests: ensure services correctly orchestrate mappers, repositories, and validations.

## Notes
- If complex conversions are needed repeatedly, prefer dedicated helper components and reference via `uses = { Helper.class }` rather than implementing business logic in mappers.
