package com.example.knowledgeforge.service;

import com.example.knowledgeforge.dao.NoteDao;
import com.example.knowledgeforge.dao.TopicDao;
import com.example.knowledgeforge.document.DocumentContainer;
import com.example.knowledgeforge.domain.note.Note;
import com.example.knowledgeforge.domain.search.SearchResultDto;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.storage.NoteFileStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Proste, pełne przeszukanie (bez indeksu) — tytuł, opis i treść (wyciągnięta
 * z .kfdoc) każdego wpisu użytkownika. Dla bazy wiedzy jednej osoby (dziesiątki/
 * setki wpisów, nie miliony) to jest szybsze do napisania i zrozumienia niż
 * jakikolwiek silnik wyszukiwania, i wystarczająco szybkie w praktyce.
 */
public class SearchService {

    private static final Logger log = Logger.getLogger(SearchService.class.getName());
    private static final int SNIPPET_RADIUS = 50;

    private final TopicDao topicDao;
    private final NoteDao noteDao;
    private final CurrentUser currentUser;
    private final NoteFileStorage fileStorage;

    public SearchService(TopicDao topicDao, NoteDao noteDao, CurrentUser currentUser, NoteFileStorage fileStorage) {
        this.topicDao = topicDao;
        this.noteDao = noteDao;
        this.currentUser = currentUser;
        this.fileStorage = fileStorage;
    }

    public List<SearchResultDto> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        Long userId = currentUser.id();

        List<Topic> topics = topicDao.findAllByUserId(userId);
        Map<UUID, Note> notesByTopic = new HashMap<>();
        for (Note note : noteDao.findAllByUserId(userId)) {
            notesByTopic.put(note.getTopicId(), note);
        }

        List<SearchResultDto> results = new ArrayList<>();
        for (Topic topic : topics) {
            SearchResultDto hit = matchTopic(topic, notesByTopic.get(topic.getId()), q);
            if (hit != null) {
                results.add(hit);
            }
        }
        log.fine(() -> "search '" + query + "': " + results.size() + " hit(s) out of " + topics.size() + " topic(s)");
        return results;
    }

    private SearchResultDto matchTopic(Topic topic, Note note, String q) {
        String title = topic.getTitle() == null ? "" : topic.getTitle();
        String description = topic.getShortPrompt() == null ? "" : topic.getShortPrompt();

        if (title.toLowerCase(Locale.ROOT).contains(q)) {
            return new SearchResultDto(topic.getId(), topic.getCategoryId(), topic.getTitle(),
                    topic.getType(), topic.getDetailLevel(), "TITLE", title);
        }
        if (description.toLowerCase(Locale.ROOT).contains(q)) {
            return new SearchResultDto(topic.getId(), topic.getCategoryId(), topic.getTitle(),
                    topic.getType(), topic.getDetailLevel(), "DESCRIPTION", description);
        }
        if (note != null) {
            String content = DocumentContainer.fromBytes(fileStorage.read(note.getContentPath())).extractPlainText();
            String lower = content.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf(q);
            if (idx >= 0) {
                return new SearchResultDto(topic.getId(), topic.getCategoryId(), topic.getTitle(),
                        topic.getType(), topic.getDetailLevel(), "CONTENT", snippet(content, idx, q.length()));
            }
        }
        return null;
    }

    private String snippet(String text, int matchIndex, int matchLength) {
        int start = Math.max(0, matchIndex - SNIPPET_RADIUS);
        int end = Math.min(text.length(), matchIndex + matchLength + SNIPPET_RADIUS);
        String s = text.substring(start, end).trim();
        return (start > 0 ? "…" : "") + s + (end < text.length() ? "…" : "");
    }
}
