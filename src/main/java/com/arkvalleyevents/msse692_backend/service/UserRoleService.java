package com.arkvalleyevents.msse692_backend.service;

import java.util.Set;

public interface UserRoleService {

    record RolesView(String firebaseUid, Set<String> roles) {}
    record RemoveRoleResult(boolean removed, String role, String uid) {}
    record SyncResult(String uid, boolean force) {}

    RolesView getRoles(String uid);

    RolesView addRoles(String uid, Set<String> rolesToAdd);

    RemoveRoleResult removeRole(String uid, String role);

    SyncResult syncClaims(String uid, boolean force);
}
