package com.possystem.communications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {
    Optional<MessageTemplate> findByMstType(String templateType);
}
