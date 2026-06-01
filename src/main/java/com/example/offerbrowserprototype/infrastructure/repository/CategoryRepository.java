package com.example.offerbrowserprototype.infrastructure.repository;

import com.example.offerbrowserprototype.domain.category.CategoryNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryNode, UUID> {

    Optional<CategoryNode> findByIdAndUserId(UUID id, Long userId);

    List<CategoryNode> findAllByUserId(Long userId);

    Optional<CategoryNode> findByUserIdAndRootTrue(Long userId);

    boolean existsByParentId(UUID parentId);

    long countByUserIdAndParentIdAndNameIgnoreCase(Long userId, UUID parentId, String name);

    boolean existsByUserIdAndParentIdIsNull(Long userId);
}
