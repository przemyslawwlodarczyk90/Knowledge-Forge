package com.example.offerbrowserprototype.infrastructure.repository;

import com.example.offerbrowserprototype.domain.offer.Offer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Testy integracyjne repozytorium ofert — H2 w pamięci, bez zewnętrznych zależności
@DataJpaTest
class OfferRepositoryIntegrationTest {

    @Autowired
    private OfferRepository repository;

    // --- helpers ---

    private Offer zapisz(String title, String url, String source, String level, LocalDateTime fetchedAt) {
        Offer offer = Offer.builder()
                .title(title)
                .description("opis")
                .location("Warsaw")
                .company("Firma")
                .offerUrl(url)
                .source(source)
                .level(level)
                .fetchedAt(fetchedAt)
                .build();
        return repository.save(offer);
    }

    // --- testy ---

    @Test
    void zapisuje_i_znajduje_oferte_po_url() {
        // given
        zapisz("Java Dev", "http://a.com/1", "NoFluff", "Junior", LocalDateTime.now());

        // when
        Optional<Offer> result = repository.findByOfferUrl("http://a.com/1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Java Dev");
    }

    @Test
    void zwraca_pusty_optional_gdy_url_nie_istnieje() {
        // when
        Optional<Offer> result = repository.findByOfferUrl("http://brak.com");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void existsByOfferUrl_zwraca_true_gdy_url_istnieje() {
        // given
        zapisz("Oferta", "http://exists.com", null, null, LocalDateTime.now());

        // when / then
        assertThat(repository.existsByOfferUrl("http://exists.com")).isTrue();
        assertThat(repository.existsByOfferUrl("http://nie-istnieje.com")).isFalse();
    }

    @Test
    void findAllByOrderByFetchedAtDesc_sortuje_od_najnowszej() {
        // given
        LocalDateTime stara = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime nowa  = LocalDateTime.of(2025, 1, 1, 10, 0);
        zapisz("Stara",  "http://old.com", null, null, stara);
        zapisz("Nowa",   "http://new.com", null, null, nowa);

        // when
        List<Offer> wynik = repository.findAllByOrderByFetchedAtDesc();

        // then — najnowsza oferta na pierwszym miejscu
        assertThat(wynik.get(0).getTitle()).isEqualTo("Nowa");
        assertThat(wynik.get(1).getTitle()).isEqualTo("Stara");
    }

    @Test
    void findDistinctSources_wyklucza_null_i_zwraca_unikalne_zrodla() {
        // given
        zapisz("O1", "http://s1.com", "NoFluff",  null, LocalDateTime.now());
        zapisz("O2", "http://s2.com", "NoFluff",  null, LocalDateTime.now());
        zapisz("O3", "http://s3.com", "Pracuj",   null, LocalDateTime.now());
        zapisz("O4", "http://s4.com", null,        null, LocalDateTime.now()); // bez źródła — wykluczone

        // when
        List<String> sources = repository.findDistinctSources();

        // then — dwa unikalne źródła, null pominięty
        assertThat(sources).containsExactlyInAnyOrder("NoFluff", "Pracuj");
        assertThat(sources).doesNotContainNull();
    }

    @Test
    void countNonDuplicate_liczy_wszystkie_gdy_brak_flag() {
        // given — brak rekordów OfferFlag, więc wszystkie oferty są liczone
        zapisz("O1", "http://c1.com", null, null, LocalDateTime.now());
        zapisz("O2", "http://c2.com", null, null, LocalDateTime.now());

        // when
        long count = repository.countNonDuplicate();

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByIdNotIn_zwraca_oferty_poza_lista_wykluczonych() {
        // given
        Offer o1 = zapisz("O1", "http://n1.com", null, null, LocalDateTime.now());
        Offer o2 = zapisz("O2", "http://n2.com", null, null, LocalDateTime.now());
        zapisz("O3", "http://n3.com", null, null, LocalDateTime.now());

        // when — wyklucz o1 i o2
        List<Offer> wynik = repository.findByIdNotIn(List.of(o1.getId(), o2.getId()));

        // then — tylko O3 w wyniku
        assertThat(wynik).hasSize(1);
        assertThat(wynik.get(0).getTitle()).isEqualTo("O3");
    }

    @Test
    void getLevelDistributionSimple_grupuje_oferty_po_poziomie() {
        // given
        zapisz("J1", "http://lv1.com", null, "Junior", LocalDateTime.now());
        zapisz("J2", "http://lv2.com", null, "Junior", LocalDateTime.now());
        zapisz("S1", "http://lv3.com", null, "Senior", LocalDateTime.now());

        // when
        var dist = repository.getLevelDistributionSimple();

        // then — dwa poziomy w wynikach
        assertThat(dist).hasSizeGreaterThanOrEqualTo(2);
    }
}
