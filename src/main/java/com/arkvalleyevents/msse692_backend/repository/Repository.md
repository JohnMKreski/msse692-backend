Repositories are Spring Data JPA interfaces that provide type‑safe, declarative access to persistence without manual SQL. They:

1. Expose CRUD operations (save, findById, delete, etc.) via JpaRepository.
2. Support pagination and sorting automatically (Page / Pageable).
3. Derive queries from method names (e.g., findByStatus) and allow @Query for custom ones.
4. Return Optional to model absence instead of null where appropriate.
5. Keep business logic out of data access; services orchestrate validation and transactions.
6. Enable testability: can be mocked or used with an in‑memory database.

In this codebase they back entities like Event, AppUser, Profile, RoleRequest, providing focused persistence operations while leaving domain rules to services and controllers.