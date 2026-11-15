package com.example.knowledgeforge.repository;

import com.example.knowledgeforge.domain.category.CategoryNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Dostęp do tabeli category_node (dawne CategoryDao). */
public interface CategoryRepository extends JpaRepository<CategoryNode, UUID> {

    Optional<CategoryNode> findByIdAndUserId(UUID id, Long userId);

    List<CategoryNode> findAllByUserId(Long userId);

    Optional<CategoryNode> findByUserIdAndRootTrue(Long userId);

    boolean existsByParentId(UUID parentId);

    @Query("select count(c) from CategoryNode c where c.userId = :userId and c.parentId = :parentId and lower(c.name) = lower(:name)")
    long countByUserIdAndParentIdAndNameIgnoreCase(@Param("userId") Long userId, @Param("parentId") UUID parentId,
                                                   @Param("name") String name);

    /** INSERT — te same wartości domyślne, co dawne CategoryDao#insert. */
    default CategoryNode insert(CategoryNode node) {
        if (node.getId() == null) node.setId(UUID.randomUUID());
        Instant now = Instant.now();
        node.setCreatedAt(now);
        node.setUpdatedAt(now);
        return save(node);
    }

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CategoryNode c set c.name = :name, c.position = :position, c.updatedAt = :updatedAt where c.id = :id")
    int updateFields(@Param("id") UUID id, @Param("name") String name, @Param("position") Integer position,
                     @Param("updatedAt") Instant updatedAt);

    default CategoryNode update(CategoryNode node) {
        node.setUpdatedAt(Instant.now());
        updateFields(node.getId(), node.getName(), node.getPosition(), node.getUpdatedAt());
        return node;
    }

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CategoryNode c where c.id = :id")
    int deleteRowById(@Param("id") UUID id);
}
