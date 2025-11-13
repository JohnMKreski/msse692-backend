package com.arkvalleyevents.msse692_backend.service;

/**
 * Service responsible for mirroring authoritative backend roles (AppUser.roles)
 * into Firebase Authentication custom claims ("roles" + optional version hash).
 */
public interface FirebaseClaimsSyncService {

    /**
     * Sync the roles for the user identified by firebaseUid into custom claims.
     * @param firebaseUid Firebase Authentication UID
     * @param force If true, push even if roles hash appears unchanged
     */
    void syncUserRolesByUid(String firebaseUid, boolean force);
}
