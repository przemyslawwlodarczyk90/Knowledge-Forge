package com.example.knowledgeforge.actuality;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.dao.TopicDao;
import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.exception.TopicNotFoundException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.domain.topic.dto.TopicDto;
import com.example.knowledgeforge.service.CurrentUser;
import com.example.knowledgeforge.ws.ApplicationEventHub;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Logika biznesowa mechanizmu okresowej weryfikacji aktualności notatek — zob.
 * dokumentacja/ACTUALITY_VERIFICATION.txt. CELOWO nie wie NIC o harmonogramie/cronie (tym zajmuje się
 * {@link ActualityVerificationScheduler}, który cyklicznie woła {@link #runVerificationSweep()})
 * — dzięki temu reguła biznesowa ("kiedy rekord traci aktualność") jest testowalna niezależnie
 * od zegara systemowego i od crona, przez wstrzyknięty {@link Clock}.
 *
 * Data bazowa dla każdego tematu to lastVerificationOfActualityDate, a w jej braku createdAt —
 * rekord traci aktualność, gdy (data bazowa + skonfigurowany okres) &lt;= aktualny czas. Okres
 * jest kalendarzowy (java.time.Period, np. "2 lata"), nie sztywna liczba dni — obliczany przez
 * odjęcie okresu od "teraz" w skonfigurowanej strefie czasowej, więc poprawnie uwzględnia lata
 * przestępne i różne długości miesięcy.
 */
public class ActualityVerificationService {

    private static final Logger log = Logger.getLogger(ActualityVerificationService.class.getName());

    private final DataSource dataSource;
    private final TopicDao topicDao;
    private final AppConfig config;
    private final CurrentUser currentUser;
    private final ApplicationEventHub eventHub;
    private final Clock clock;

    public ActualityVerificationService(DataSource dataSource, TopicDao topicDao, AppConfig config,
                                         CurrentUser currentUser, ApplicationEventHub eventHub, Clock clock) {
        this.dataSource = dataSource;
        this.topicDao = topicDao;
        this.config = config;
        this.currentUser = currentUser;
        this.eventHub = eventHub;
        this.clock = clock;
    }

    /**
     * GET /api/topics/actuality-review[?author=...] — wyłącznie rekordy actualityVerified=false,
     * należące do bieżącego użytkownika, opcjonalnie zawężone po autorze, posortowane od
     * najstarszej daty bazowej. Nie czyta treści .kfdoc.
     */
    public List<TopicDto> listForReview(String author) {
        Long userId = currentUser.id();
        String normalizedAuthor = (author == null || author.isBlank()) ? null : author;
        return topicDao.findForActualityReview(userId, normalizedAuthor).stream()
                .map(TopicDto::from)
                .collect(Collectors.toList());
    }

    /**
     * POST /api/topics/{id}/verify-actuality — ręczne, świadome potwierdzenie aktualności.
     * Optimistic locking na Topic#version (dokładnie jak TopicService#update): niezgodna
     * expectedVersion -> ConflictException (409), 0 zmienionych wierszy w bazie. Zdarzenie
     * WebSocket leci dopiero PO udanym (autocommitted) UPDATE-cie.
     */
    public TopicDto confirmActuality(UUID topicId, Integer expectedVersion, String clientId) {
        if (expectedVersion == null) {
            throw new ValidationException("expectedVersion must not be null");
        }
        Long userId = currentUser.id();
        Topic topic = topicDao.findByIdAndUserId(topicId, userId)
                .orElseThrow(() -> new TopicNotFoundException(topicId.toString()));

        Instant verifiedAt = clock.instant();
        int affected;
        try (Connection con = dataSource.getConnection()) {
            affected = topicDao.confirmActuality(con, topicId, expectedVersion, verifiedAt);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open connection for actuality confirmation", e);
        }
        if (affected == 0) {
            log.warning(() -> "Actuality confirmation conflict for topic " + topicId
                    + " (expectedVersion=" + expectedVersion + ")");
            throw new ConflictException("Topic was modified by someone else in the meantime");
        }

        topic.setActualityVerified(true);
        topic.setLastVerificationOfActualityDate(verifiedAt);
        topic.setVersion(expectedVersion + 1);
        log.info(() -> "Actuality confirmed for topic " + topicId + " -> version=" + topic.getVersion());

        TopicDto dto = TopicDto.from(topic);
        eventHub.topicUpdated(dto, String.valueOf(userId), clientId);
        eventHub.actualityReviewChanged(1, List.of(topicId));
        return dto;
    }

    /**
     * Woła ActualityVerificationScheduler raz na uruchomienie crona (i bezpośrednio testy
     * jednostkowe — sama logika nie zależy od crona). Zwraca liczbę tematów faktycznie
     * oznaczonych jako nieaktualne w tym przebiegu. Nie czyta treści .kfdoc — cała operacja to
     * jeden zbiorczy UPDATE w TopicDao#markStaleAsUnverified.
     */
    public int runVerificationSweep() {
        Period period = config.actualityVerificationPeriod();
        ZoneId zone = config.actualityVerificationZoneId();
        Instant now = clock.instant();
        Instant cutoff = ZonedDateTime.ofInstant(now, zone).minus(period).toInstant();
        log.fine(() -> "Actuality sweep: period=" + period + " zone=" + zone + " cutoff=" + cutoff);

        List<Topic> changed = topicDao.markStaleAsUnverified(cutoff);
        if (!changed.isEmpty()) {
            List<UUID> ids = new ArrayList<>(changed.size());
            for (Topic t : changed) {
                ids.add(t.getId());
                // Aktor systemowy — nie kojarzony z żadną konkretną kartą/użytkownikiem, więc
                // WSZYSTKIE otwarte karty (łącznie z tą, która akurat patrzy na ten temat)
                // poprawnie zastosują świeży `version`, zamiast zignorować zdarzenie jako "własne".
                eventHub.topicUpdated(TopicDto.from(t), null, null);
            }
            eventHub.actualityReviewChanged(changed.size(), ids);
        }
        return changed.size();
    }
}
