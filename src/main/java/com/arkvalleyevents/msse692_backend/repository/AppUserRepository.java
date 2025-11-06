package com.arkvalleyevents.msse692_backend.repository;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByFirebaseUid(String firebaseUid);
    boolean existsByFirebaseUid(String firebaseUid);
}
