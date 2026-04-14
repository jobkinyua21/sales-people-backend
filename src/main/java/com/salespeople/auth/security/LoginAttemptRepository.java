package com.salespeople.auth.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    List<LoginAttempt> findByUsrIdAndAttemptedAtAfterOrderByAttemptedAtDesc(UUID usrId, LocalDateTime after);

    long countByUsrIdAndSuccessFalseAndAttemptedAtAfter(UUID usrId, LocalDateTime after);

    List<LoginAttempt> findByEmailOrderByAttemptedAtDesc(String email);
}
