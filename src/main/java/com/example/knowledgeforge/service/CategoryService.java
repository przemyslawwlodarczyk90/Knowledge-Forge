package com.example.knowledgeforge.service;

import com.example.knowledgeforge.dao.CategoryDao;
import com.example.knowledgeforge.dao.TopicDao;
import com.example.knowledgeforge.domain.category.CategoryNode;
import com.example.knowledgeforge.domain.category.dto.CategoryDto;
import com.example.knowledgeforge.domain.category.dto.CategoryTreeDto;
import com.example.knowledgeforge.domain.category.dto.CreateCategoryRequest;
import com.example.knowledgeforge.domain.category.dto.UpdateCategoryRequest;
import com.example.knowledgeforge.domain.exception.CategoryNotFoundException;
import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.exception.ForbiddenException;
import com.example.knowledgeforge.domain.exception.ValidationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CategoryService {

    private final CategoryDao categoryDao;
    private final TopicDao topicDao;
    private final CurrentUser currentUser;

    public CategoryService(CategoryDao categoryDao, TopicDao topicDao, CurrentUser currentUser) {
        this.categoryDao = categoryDao;
        this.topicDao = topicDao;
        this.currentUser = currentUser;
    }

    public CategoryTreeDto getTree() {
        Long userId = currentUser.id();
        CategoryNode root = categoryDao.findByUserIdAndRootTrue(userId)
                .orElseGet(() -> createVirtualRoot(userId));
        List<CategoryNode> all = categoryDao.findAllByUserId(userId);
        Map<UUID, Integer> topicCounts = topicDao.countByCategoryForUser(userId);
        Map<UUID, Integer> lowDetailCounts = topicDao.countLowDetailByCategoryForUser(userId);
        return buildTree(root, all, topicCounts, lowDetailCounts);
    }

    public CategoryDto create(CreateCategoryRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ValidationException("name must not be blank");
        }

        Long userId = currentUser.id();

        CategoryNode root = categoryDao.findByUserIdAndRootTrue(userId)
                .orElseGet(() -> createVirtualRoot(userId));

        UUID parentId = req.parentId() != null ? req.parentId() : root.getId();

        categoryDao.findByIdAndUserId(parentId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(parentId.toString()));

        long nameCount = categoryDao.countByUserIdAndParentIdAndNameIgnoreCase(userId, parentId, req.name());
        if (nameCount > 0) {
            throw new ConflictException("A category with name '" + req.name() + "' already exists at this level.");
        }

        CategoryNode node = new CategoryNode();
        node.setUserId(userId);
        node.setParentId(parentId);
        node.setName(req.name());
        node.setPosition(0);
        node.setRoot(false);

        return CategoryDto.from(categoryDao.insert(node));
    }

    public CategoryDto update(UUID id, UpdateCategoryRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ValidationException("name must not be blank");
        }

        Long userId = currentUser.id();
        CategoryNode node = categoryDao.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));

        if (node.isRoot()) {
            throw new ForbiddenException("Cannot modify the virtual root node.");
        }

        node.setName(req.name());

        return CategoryDto.from(categoryDao.update(node));
    }

    public void delete(UUID id) {
        Long userId = currentUser.id();
        CategoryNode node = categoryDao.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));

        if (node.isRoot()) {
            throw new ForbiddenException("Cannot delete the virtual root node.");
        }

        if (categoryDao.existsByParentId(id)) {
            throw new ConflictException("Category has sub-categories. Remove them first.");
        }

        if (topicDao.existsByUserIdAndCategoryId(userId, id)) {
            throw new ConflictException("Category has topics. Remove them first.");
        }

        categoryDao.delete(id);
    }

    private CategoryNode createVirtualRoot(Long userId) {
        CategoryNode root = new CategoryNode();
        root.setUserId(userId);
        root.setName("__ROOT__");
        root.setPosition(0);
        root.setRoot(true);
        return categoryDao.insert(root);
    }

    private CategoryTreeDto buildTree(CategoryNode root, List<CategoryNode> all, Map<UUID, Integer> topicCounts,
                                       Map<UUID, Integer> lowDetailCounts) {
        Map<UUID, List<CategoryNode>> byParent = all.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryNode::getParentId));

        return CategoryTreeDto.fromRoot(root, byParent, topicCounts, lowDetailCounts);
    }
}
