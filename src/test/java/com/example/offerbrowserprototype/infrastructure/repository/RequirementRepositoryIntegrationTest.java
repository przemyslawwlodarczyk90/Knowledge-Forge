package com.example.offerbrowserprototype.infrastructure.repository;

import com.example.offerbrowserprototype.domain.requirement.Requirement;
import com.example.offerbrowserprototype.domain.requirement.SkillCountProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Testy integracyjne repozytorium wymagań — H2 w pamięci, weryfikują zapytania JPQL
@DataJpaTest
class RequirementRepositoryIntegrationTest {

    @Autowired
    private RequirementRepository repository;

    // --- helpers ---

    private void zapisz(String skill, String level, String source) {
        repository.save(Requirement.builder()
                .skill(skill)
                .level(level)
                .source(source)
                .build());
    }

    // --- testy ---

    @Test
    void findTopSkills_sortuje_malejaco_po_liczbie_wystapien() {
        // given — Java pojawia się 3 razy, Python 1 raz
        zapisz("Java", "Junior", "NoFluff");
        zapisz("Java", "Senior", "NoFluff");
        zapisz("Java", null,     "NoFluff");
        zapisz("Python", "Mid",  "Pracuj");

        // when
        List<SkillCountProjection> wynik = repository.findTopSkills(PageRequest.of(0, 10));

        // then — Java na pierwszym miejscu
        assertThat(wynik).isNotEmpty();
        assertThat(wynik.get(0).getSkill()).isEqualTo("Java");
        assertThat(wynik.get(0).getCount()).isEqualTo(3L);
    }

    @Test
    void findTopSkillsBySources_filtruje_po_zrodle() {
        // given
        zapisz("Java",   "Junior", "NoFluff");
        zapisz("Kotlin", "Senior", "Pracuj");  // inne źródło — wykluczone

        // when
        List<SkillCountProjection> wynik = repository.findTopSkillsBySources(
                List.of("NoFluff"), PageRequest.of(0, 10));

        // then — tylko Java z NoFluff
        assertThat(wynik).hasSize(1);
        assertThat(wynik.get(0).getSkill()).isEqualTo("Java");
    }

    @Test
    void findTopSkillsByLevel_zwraca_skille_dla_podanego_poziomu() {
        // given
        zapisz("Java",   "Junior", "NoFluff");
        zapisz("Python", "Senior", "NoFluff"); // inny poziom — wykluczone

        // when
        List<SkillCountProjection> wynik = repository.findTopSkillsByLevel("Junior", PageRequest.of(0, 10));

        // then
        assertThat(wynik).hasSize(1);
        assertThat(wynik.get(0).getSkill()).isEqualTo("Java");
    }

    @Test
    void findTopSkillsByLevelAndSources_filtruje_po_poziomie_i_zrodle() {
        // given
        zapisz("Java",   "Junior", "NoFluff");
        zapisz("Java",   "Junior", "Pracuj");  // wykluczone — inne źródło
        zapisz("Python", "Senior", "NoFluff"); // wykluczone — inny poziom

        // when
        List<SkillCountProjection> wynik = repository.findTopSkillsByLevelAndSources(
                "Junior", List.of("NoFluff"), PageRequest.of(0, 10));

        // then — tylko Java + Junior + NoFluff
        assertThat(wynik).hasSize(1);
        assertThat(wynik.get(0).getSkill()).isEqualTo("Java");
        assertThat(wynik.get(0).getCount()).isEqualTo(1L);
    }

    @Test
    void findTopSkillsByNullLevel_zwraca_skille_z_brakujacym_poziomem() {
        // given
        zapisz("Java",   null,     "NoFluff"); // level=null — wliczone
        zapisz("Python", "Junior", "NoFluff"); // ma poziom — wykluczone

        // when
        List<SkillCountProjection> wynik = repository.findTopSkillsByNullLevel(PageRequest.of(0, 10));

        // then
        assertThat(wynik).hasSize(1);
        assertThat(wynik.get(0).getSkill()).isEqualTo("Java");
    }

    @Test
    void findTopSkillsByNullLevelAndSources_filtruje_null_poziom_i_zrodlo() {
        // given
        zapisz("Java",   null, "NoFluff");
        zapisz("Kotlin", null, "Pracuj"); // inne źródło — wykluczone

        // when
        List<SkillCountProjection> wynik = repository.findTopSkillsByNullLevelAndSources(
                List.of("NoFluff"), PageRequest.of(0, 10));

        // then
        assertThat(wynik).hasSize(1);
        assertThat(wynik.get(0).getSkill()).isEqualTo("Java");
    }

    @Test
    void findDistinctLevels_wyklucza_null_i_zwraca_unikalne_poziomy() {
        // given
        zapisz("Java",   "Junior", "NoFluff");
        zapisz("Python", "Junior", "NoFluff"); // duplikat poziomu
        zapisz("Go",     "Senior", "Pracuj");
        zapisz("Rust",   null,     "NoFluff"); // null — wykluczone

        // when
        List<String> poziomy = repository.findDistinctLevels();

        // then — dwa unikalne poziomy, bez null
        assertThat(poziomy).containsExactlyInAnyOrder("Junior", "Senior");
        assertThat(poziomy).doesNotContainNull();
    }

    @Test
    void existsNullLevel_zwraca_true_gdy_istnieje_wymaganie_bez_poziomu() {
        // given
        zapisz("Java", null, "NoFluff");

        // when / then
        assertThat(repository.existsNullLevel()).isTrue();
    }

    @Test
    void existsNullLevel_zwraca_false_gdy_wszystkie_wymagania_maja_poziom() {
        // given — wszystkie wymagania z poziomem
        zapisz("Java",   "Junior", "NoFluff");
        zapisz("Python", "Senior", "Pracuj");

        // when / then
        assertThat(repository.existsNullLevel()).isFalse();
    }
}
