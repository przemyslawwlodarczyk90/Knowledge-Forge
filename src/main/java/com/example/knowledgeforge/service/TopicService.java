package com.example.knowledgeforge.service;

import com.example.knowledgeforge.dao.CategoryDao;
import com.example.knowledgeforge.dao.TopicDao;
import com.example.knowledgeforge.domain.category.CategoryNode;
import com.example.knowledgeforge.domain.exception.CategoryNotFoundException;
import com.example.knowledgeforge.domain.exception.TopicNotFoundException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.domain.topic.TopicStatus;
import com.example.knowledgeforge.domain.topic.dto.CreateTopicRequest;
import com.example.knowledgeforge.domain.topic.dto.TopicDto;
import com.example.knowledgeforge.domain.topic.dto.UpdateTopicRequest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TopicService {

    private static final Logger log = Logger.getLogger(TopicService.class.getName());

    private final TopicDao topicDao;
    private final CategoryDao categoryDao;
    private final CurrentUser currentUser;

    public TopicService(TopicDao topicDao, CategoryDao categoryDao, CurrentUser currentUser) {
        this.topicDao = topicDao;
        this.categoryDao = categoryDao;
        this.currentUser = currentUser;
    }

    public TopicDto create(CreateTopicRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new ValidationException("title must not be blank");
        }
        if (req.detailLevel() == null) {
            throw new ValidationException("detailLevel must not be null");
        }
        if (req.type() == null) {
            throw new ValidationException("type must not be null");
        }
        if (req.categoryId() == null) {
            throw new ValidationException("categoryId must not be null");
        }

        Long userId = currentUser.id();

        categoryDao.findByIdAndUserId(req.categoryId(), userId)
                .orElseThrow(() -> new CategoryNotFoundException(req.categoryId().toString()));

        Topic topic = new Topic();
        topic.setUserId(userId);
        topic.setCategoryId(req.categoryId());
        topic.setTitle(req.title());
        topic.setShortPrompt(req.shortPrompt());
        topic.setAuthor(blankToNull(req.author()));
        topic.setDetailLevel(req.detailLevel());
        topic.setType(req.type());
        topic.setStatus(TopicStatus.NEW);

        Topic saved = topicDao.insert(topic);
        log.info(() -> "Created topic " + saved.getId() + " (" + saved.getType() + ", " + saved.getDetailLevel() + ")");
        return TopicDto.from(saved);
    }

    public TopicDto getById(UUID id) {
        Long userId = currentUser.id();
        Topic topic = topicDao.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TopicNotFoundException(id.toString()));
        return TopicDto.from(topic);
    }

    public List<TopicDto> listByCategory(UUID categoryId) {
        Long userId = currentUser.id();

        categoryDao.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId.toString()));

        return topicDao.findAllByUserIdAndCategoryIdOrderByCreatedAtDesc(userId, categoryId)
                .stream()
                .map(TopicDto::from)
                .collect(Collectors.toList());
    }

    /** Autorzy użyci choć raz przez użytkownika — zasila listę wyboru w panelu filtrów. */
    public List<String> listAuthors() {
        return topicDao.findDistinctAuthors(currentUser.id());
    }

    /**
     * Proste, pełne przeszukanie po filtrach (bez indeksu) — spójne z podejściem SearchService.
     * categoryId obejmuje też wszystkie podkategorie wybranej kategorii (filtr "w dół" drzewa).
     * Bez żadnego filtra świadomie zwraca pustą listę, żeby przypadkowe wywołanie nie zrzucało
     * całej bazy wiedzy.
     */
    public List<TopicDto> filter(String author, DetailLevel detailLevel, UUID categoryId) {
        if (author == null && detailLevel == null && categoryId == null) {
            return List.of();
        }
        Long userId = currentUser.id();
        Set<UUID> categoryScope = categoryId == null ? null : resolveCategoryScope(userId, categoryId);

        return topicDao.findAllByUserId(userId).stream()
                .filter(t -> author == null || author.equalsIgnoreCase(t.getAuthor()))
                .filter(t -> detailLevel == null || detailLevel == t.getDetailLevel())
                .filter(t -> categoryScope == null || categoryScope.contains(t.getCategoryId()))
                .map(TopicDto::from)
                .collect(Collectors.toList());
    }

    /** Wybrana kategoria + wszystkie jej podkategorie, dowolnej głębokości (obchód w głąb). */
    private Set<UUID> resolveCategoryScope(Long userId, UUID categoryId) {
        Map<UUID, List<CategoryNode>> byParent = categoryDao.findAllByUserId(userId).stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryNode::getParentId));

        Set<UUID> scope = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(categoryId);
        while (!stack.isEmpty()) {
            UUID id = stack.pop();
            if (!scope.add(id)) continue;
            for (CategoryNode child : byParent.getOrDefault(id, List.of())) {
                stack.push(child.getId());
            }
        }
        return scope;
    }

    public TopicDto update(UUID id, UpdateTopicRequest req) {
        Long userId = currentUser.id();
        Topic topic = topicDao.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TopicNotFoundException(id.toString()));

        if (req.title() != null && !req.title().isBlank()) {
            topic.setTitle(req.title());
        }
        if (req.shortPrompt() != null) {
            topic.setShortPrompt(req.shortPrompt());
        }
        // Pusty string = świadome wyczyszczenie autora; null = pole nieobecne w tym częściowym update.
        if (req.author() != null) {
            topic.setAuthor(blankToNull(req.author()));
        }
        if (req.detailLevel() != null) {
            topic.setDetailLevel(req.detailLevel());
        }
        if (req.type() != null) {
            topic.setType(req.type());
        }

        Topic saved = topicDao.update(topic);
        log.fine(() -> "Updated topic " + saved.getId());
        return TopicDto.from(saved);
    }

    public void delete(UUID id) {
        Long userId = currentUser.id();
        Topic topic = topicDao.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TopicNotFoundException(id.toString()));
        topicDao.delete(topic.getId());
        log.info(() -> "Deleted topic " + id);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /** Używane przez NoteService do potwierdzenia własności tematu. */
    Topic verifyOwnership(UUID topicId, Long userId) {
        return topicDao.findByIdAndUserId(topicId, userId)
                .orElseThrow(() -> new TopicNotFoundException(topicId.toString()));
    }

    void save(Topic topic) {
        topicDao.update(topic);
    }
}
