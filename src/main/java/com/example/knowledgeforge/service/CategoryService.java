package com.example.knowledgeforge.service;

import com.example.knowledgeforge.domain.category.CategoryNode;
import com.example.knowledgeforge.domain.category.dto.CategoryDto;
import com.example.knowledgeforge.domain.category.dto.CategoryTreeDto;
import com.example.knowledgeforge.domain.category.dto.CreateCategoryRequest;
import com.example.knowledgeforge.domain.category.dto.UpdateCategoryRequest;
import com.example.knowledgeforge.domain.exception.CategoryNotFoundException;
import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.exception.ForbiddenException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.repository.CategoryRepository;
import com.example.knowledgeforge.repository.TopicRepository;
import com.example.knowledgeforge.ws.ApplicationEventHub;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TopicRepository topicRepository;
    private final CurrentUser currentUser;
    private final ApplicationEventHub eventHub;

    public CategoryService(CategoryRepository categoryRepository, TopicRepository topicRepository, CurrentUser currentUser, ApplicationEventHub eventHub) {
        this.categoryRepository = categoryRepository;
        this.topicRepository = topicRepository;
        this.currentUser = currentUser;
        this.eventHub = eventHub;
    }

    public CategoryTreeDto getTree() {
        Long userId = currentUser.id();
        CategoryNode root = categoryRepository.findByUserIdAndRootTrue(userId)
                .orElseGet(() -> createVirtualRoot(userId));
        List<CategoryNode> all = categoryRepository.findAllByUserId(userId);
        Map<UUID, Integer> topicCounts = topicRepository.countByCategoryForUser(userId);
        Map<UUID, Integer> lowDetailCounts = topicRepository.countLowDetailByCategoryForUser(userId);
        return buildTree(root, all, topicCounts, lowDetailCounts);
    }

    public CategoryDto create(CreateCategoryRequest req, String clientId) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ValidationException("name must not be blank");
        }

        Long userId = currentUser.id();

        CategoryNode root = categoryRepository.findByUserIdAndRootTrue(userId)
                .orElseGet(() -> createVirtualRoot(userId));

        UUID parentId = req.parentId() != null ? req.parentId() : root.getId();

        categoryRepository.findByIdAndUserId(parentId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(parentId.toString()));

        long nameCount = categoryRepository.countByUserIdAndParentIdAndNameIgnoreCase(userId, parentId, req.name());
        if (nameCount > 0) {
            throw new ConflictException("A category with name '" + req.name() + "' already exists at this level.");
        }

        CategoryNode node = new CategoryNode();
        node.setUserId(userId);
        node.setParentId(parentId);
        node.setName(req.name());
        node.setPosition(0);
        node.setRoot(false);

        CategoryDto dto = CategoryDto.from(categoryRepository.insert(node));
        eventHub.categoryCreated(userId, dto, String.valueOf(userId), clientId);
        return dto;
    }

    public CategoryDto update(UUID id, UpdateCategoryRequest req, String clientId) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ValidationException("name must not be blank");
        }

        Long userId = currentUser.id();
        CategoryNode node = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));

        if (node.isRoot()) {
            throw new ForbiddenException("Cannot modify the virtual root node.");
        }

        node.setName(req.name());

        CategoryDto dto = CategoryDto.from(categoryRepository.update(node));
        eventHub.categoryUpdated(userId, dto, String.valueOf(userId), clientId);
        return dto;
    }

    public void delete(UUID id, String clientId) {
        Long userId = currentUser.id();
        CategoryNode node = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));

        if (node.isRoot()) {
            throw new ForbiddenException("Cannot delete the virtual root node.");
        }

        if (categoryRepository.existsByParentId(id)) {
            throw new ConflictException("Category has sub-categories. Remove them first.");
        }

        if (topicRepository.existsByUserIdAndCategoryId(userId, id)) {
            throw new ConflictException("Category has topics. Remove them first.");
        }

        categoryRepository.deleteRowById(id);
        eventHub.categoryDeleted(userId, id, node.getParentId(), String.valueOf(userId), clientId);
    }

    private CategoryNode createVirtualRoot(Long userId) {
        CategoryNode root = new CategoryNode();
        root.setUserId(userId);
        root.setName("__ROOT__");
        root.setPosition(0);
        root.setRoot(true);
        return categoryRepository.insert(root);
    }

    private CategoryTreeDto buildTree(CategoryNode root, List<CategoryNode> all, Map<UUID, Integer> topicCounts,
                                       Map<UUID, Integer> lowDetailCounts) {
        Map<UUID, List<CategoryNode>> byParent = all.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryNode::getParentId));

        return CategoryTreeDto.fromRoot(root, byParent, topicCounts, lowDetailCounts);
    }
}
