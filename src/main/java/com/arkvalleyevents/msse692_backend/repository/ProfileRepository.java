package com.arkvalleyevents.msse692_backend.repository;

import com.arkvalleyevents.msse692_backend.model.Profile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserId(Long userId);

    @Query("select p from Profile p join p.user u where u.firebaseUid = :firebaseUid")
    Optional<Profile> findByUserFirebaseUid(@Param("firebaseUid") String firebaseUid);

    boolean existsByUserId(Long userId);
}
