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
import com.example.knowledgeforge.ws.ApplicationEventHub;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TopicService {

    private static final Logger log = Logger.getLogger(TopicService.class.getName());

    private final DataSource dataSource;
    private final TopicDao topicDao;
    private final CategoryDao categoryDao;
    private final CurrentUser currentUser;
    private final AttachmentService attachmentService;
    private final ApplicationEventHub eventHub;

    public TopicService(DataSource dataSource, TopicDao topicDao, CategoryDao categoryDao, CurrentUser currentUser,
                         AttachmentService attachmentService, ApplicationEventHub eventHub) {
        this.dataSource = dataSource;
        this.topicDao = topicDao;
        this.categoryDao = categoryDao;
        this.currentUser = currentUser;
        this.attachmentService = attachmentService;
        this.eventHub = eventHub;
    }

    /**
     * Tworzy temat i od razu liczy aktualną (bezpośrednią) liczbę tematów w tej kategorii —
     * w JEDNEJ transakcji JDBC, żeby COUNT(*) widział własny insert (ta sama sesja = read-committed
     * widzi własne, jeszcze niezacommitowane zmiany) i żeby żaden inny request nie mógł "wcisnąć się"
     * między insert a odczyt licznika z inną, sprzeczną wartością. Zdarzenie WS leci dopiero po commicie.
     */
    public TopicDto create(CreateTopicRequest req, String clientId) {
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

        int categoryTopicCount;
        // MULTI-THREADING:
        // Jedna transakcja JDBC (autoCommit=false) obejmuje insert tematu ORAZ odczyt aktualnego
        // licznika tej kategorii. Chroni to przed niespójnością, gdy wiele żądań Jetty (różne
        // wątki puli HikariCP) tworzy tematy w tej samej kategorii równolegle: dzięki temu, że
        // COUNT(*) jest częścią TEJ SAMEJ transakcji co insert, na pewno widzi własny wiersz
        // (read-committed zawsze widzi własne, niezacommitowane zmiany), a commit/rollback są
        // atomowe — nigdy nie zwrócimy licznika bez faktycznie zapisanego tematu ani odwrotnie.
        // Blokada nie jest szersza niż to (insert + jeden COUNT) — żadna komunikacja sieciowa
        // (WebSocket) nie dzieje się, dopóki transakcja trwa; publikacja zdarzenia jest PO commit,
        // poza zakresem połączenia z bazą. Transakcja jest zwalniana (commit/rollback + close) w
        // finally, zanim metoda cokolwiek rozgłosi.
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try {
                Topic saved = topicDao.insert(con, topic);
                categoryTopicCount = topicDao.countDirectByCategoryId(con, saved.getCategoryId(), userId);
                con.commit();
            } catch (Exception e) {
                safeRollback(con);
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open transaction for topic creation", e);
        }

        log.info(() -> "Created topic " + topic.getId() + " (" + topic.getType() + ", " + topic.getDetailLevel()
                + "), category now has " + categoryTopicCount + " topic(s)");

        TopicDto dto = TopicDto.from(topic);
        dto.setCategoryTopicCount(categoryTopicCount);

        String actorId = String.valueOf(userId);
        eventHub.topicCreated(dto, categoryTopicCount, actorId, clientId);

        return dto;
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

    public TopicDto update(UUID id, UpdateTopicRequest req, String clientId) {
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

        // MULTI-THREADING:
        // Optimistic locking (bez żadnego locka w pamięci) — TopicDao#update robi
        // UPDATE ... WHERE id = ? AND version = ?, version = version + 1 jednym atomowym
        // zapytaniem SQL. Gdy req.expectedVersion() nie zgadza się z aktualną wartością w bazie
        // (bo ktoś inny zapisał ten temat pomiędzy odczytem a tym zapisem), 0 zmienionych wierszy
        // -> ConflictException -> HTTP 409. Nie blokuje to innych użytkowników podczas edycji —
        // każdy może próbować zapisać w dowolnej chwili, konflikt wykrywa się dopiero na samym
        // zapisie, nie przez wcześniejsze zablokowanie rekordu.
        Topic saved = topicDao.update(topic, req.expectedVersion());
        log.fine(() -> "Updated topic " + saved.getId());

        TopicDto dto = TopicDto.from(saved);
        eventHub.topicUpdated(dto, String.valueOf(userId), clientId);
        return dto;
    }

    public void delete(UUID id, String clientId) {
        Long userId = currentUser.id();
        Topic topic = topicDao.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TopicNotFoundException(id.toString()));
        // Pliki załączników muszą zniknąć w kontrolowany sposób (z logowaniem niepowodzeń) ZANIM
        // skasujemy sam temat — rekordy attachment i tak skasują się kaskadowo (ON DELETE CASCADE),
        // ale to nie ruszy plików na dysku, więc robimy to jawnie tutaj.
        attachmentService.deleteFilesForTopic(id);
        topicDao.delete(topic.getId());
        log.info(() -> "Deleted topic " + id);

        int remaining;
        try (Connection con = dataSource.getConnection()) {
            remaining = topicDao.countDirectByCategoryId(con, topic.getCategoryId(), userId);
        } catch (SQLException e) {
            remaining = -1; // nie krytyczne dla samego usunięcia — frontend i tak ma reconnect-resync
        }
        eventHub.topicDeleted(id, topic.getCategoryId(), remaining, String.valueOf(userId), clientId);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static void safeRollback(Connection con) {
        try {
            con.rollback();
        } catch (SQLException ignored) {
            // najlepszy wysiłek — połączenie i tak zostanie zamknięte (try-with-resources)
        }
    }

    /** Używane przez NoteService do potwierdzenia własności tematu. */
    Topic verifyOwnership(UUID topicId, Long userId) {
        return topicDao.findByIdAndUserId(topicId, userId)
                .orElseThrow(() -> new TopicNotFoundException(topicId.toString()));
    }
}
