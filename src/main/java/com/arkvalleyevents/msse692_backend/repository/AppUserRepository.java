package com.arkvalleyevents.msse692_backend.repository;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByFirebaseUid(String firebaseUid);
    boolean existsByFirebaseUid(String firebaseUid);

    @Query("SELECT DISTINCT u FROM AppUser u LEFT JOIN u.roles r " +
           "WHERE (:text IS NULL OR (LOWER(u.email) LIKE LOWER(CONCAT('%',:text,'%')) " +
           " OR LOWER(u.displayName) LIKE LOWER(CONCAT('%',:text,'%')) " +
           " OR LOWER(u.firebaseUid) LIKE LOWER(CONCAT('%',:text,'%')))) " +
           "AND (:rolesEmpty = TRUE OR r IN :roles)")
    Page<AppUser> search(@Param("text") String text,
                         @Param("roles") Set<String> roles,
                         @Param("rolesEmpty") boolean rolesEmpty,
                         Pageable pageable);
}
