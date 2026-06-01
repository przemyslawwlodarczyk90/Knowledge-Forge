package com.example.offerbrowserprototype.infrastructure.facade;

import com.example.offerbrowserprototype.domain.category.CategoryNode;
import com.example.offerbrowserprototype.domain.category.dto.CategoryDto;
import com.example.offerbrowserprototype.domain.category.dto.CategoryTreeDto;
import com.example.offerbrowserprototype.domain.category.dto.CreateCategoryRequest;
import com.example.offerbrowserprototype.domain.category.dto.UpdateCategoryRequest;
import com.example.offerbrowserprototype.domain.exception.CategoryNotFoundException;
import com.example.offerbrowserprototype.domain.exception.ConflictException;
import com.example.offerbrowserprototype.domain.exception.ForbiddenException;
import com.example.offerbrowserprototype.infrastructure.repository.CategoryRepository;
import com.example.offerbrowserprototype.infrastructure.repository.TopicRepository;
import com.example.offerbrowserprototype.infrastructure.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryFacade {

    private final CategoryRepository repo;
    private final TopicRepository topicRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public CategoryTreeDto getTree() {
        Long userId = currentUser.id();
        CategoryNode root = repo.findByUserIdAndRootTrue(userId)
                .orElseGet(() -> createVirtualRoot(userId));
        List<CategoryNode> all = repo.findAllByUserId(userId);
        return buildTree(root, all);
    }

    public CategoryDto create(CreateCategoryRequest req) {
        Long userId = currentUser.id();

        // Ensure root exists
        CategoryNode root = repo.findByUserIdAndRootTrue(userId)
                .orElseGet(() -> createVirtualRoot(userId));

        UUID parentId = req.parentId() != null ? req.parentId() : root.getId();

        // Verify parent belongs to this user
        repo.findByIdAndUserId(parentId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(parentId.toString()));

        // Name uniqueness check within same parent
        long nameCount = repo.countByUserIdAndParentIdAndNameIgnoreCase(userId, parentId, req.name());
        if (nameCount > 0) {
            throw new ConflictException("A category with name '" + req.name() + "' already exists at this level.");
        }

        CategoryNode node = CategoryNode.builder()
                .userId(userId)
                .parentId(parentId)
                .name(req.name())
                .description(req.description())
                .position(0)
                .root(false)
                .build();

        return CategoryDto.from(repo.save(node));
    }

    public CategoryDto update(UUID id, UpdateCategoryRequest req) {
        Long userId = currentUser.id();
        CategoryNode node = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));

        if (node.isRoot()) {
            throw new ForbiddenException("Cannot modify the virtual root node.");
        }

        node.setName(req.name());
        node.setDescription(req.description());

        return CategoryDto.from(repo.save(node));
    }

    public void delete(UUID id) {
        Long userId = currentUser.id();
        CategoryNode node = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));

        if (node.isRoot()) {
            throw new ForbiddenException("Cannot delete the virtual root node.");
        }

        if (repo.existsByParentId(id)) {
            throw new ConflictException("Category has sub-categories. Remove them first.");
        }

        if (topicRepository.existsByUserIdAndCategoryId(userId, id)) {
            throw new ConflictException("Category has topics. Remove them first.");
        }

        repo.delete(node);
    }

    private CategoryNode createVirtualRoot(Long userId) {
        CategoryNode root = CategoryNode.builder()
                .userId(userId)
                .name("__ROOT__")
                .description("Virtual root node")
                .position(0)
                .root(true)
                .build();
        return repo.save(root);
    }

    private CategoryTreeDto buildTree(CategoryNode root, List<CategoryNode> all) {
        Map<UUID, List<CategoryNode>> byParent = all.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryNode::getParentId));

        return CategoryTreeDto.fromRoot(root, byParent);
    }
}
