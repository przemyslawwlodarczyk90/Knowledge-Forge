package com.example.offerbrowserprototype.infrastructure.repository;

import com.example.offerbrowserprototype.domain.ai.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    Optional<AiUsage> findByUserIdAndUsageDate(Long userId, LocalDate date);
}
