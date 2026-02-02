package com.possystem.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByProfileCode(String profileCode);

    Optional<Profile> findByProfileName(String profileName);

    boolean existsByProfileCode(String profileCode);

    boolean existsByProfileName(String profileName);
}
