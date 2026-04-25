package com.salespeople.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTbRepository extends JpaRepository<UserTb, Long> {

    Optional<UserTb> findByUserEmail(String userEmail);

    boolean existsByUserEmail(String userEmail);

    boolean existsByStaffNumber(Integer staffNumber);
}
