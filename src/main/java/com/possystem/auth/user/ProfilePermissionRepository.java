package com.possystem.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfilePermissionRepository extends JpaRepository<ProfilePermission, UUID> {

    List<ProfilePermission> findByProfile(Profile profile);

    List<ProfilePermission> findByProfileProfileId(UUID profileId);

    Optional<ProfilePermission> findByProfileAndPermission(Profile profile, Permission permission);

    Optional<ProfilePermission> findByProfileProfileIdAndPermissionPermissionId(UUID profileId, UUID permissionId);

    void deleteByProfileProfileId(UUID profileId);
}
