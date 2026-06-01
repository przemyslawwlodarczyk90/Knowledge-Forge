package com.example.offerbrowserprototype.domain.requirement;

import com.example.offerbrowserprototype.domain.dto.analytics.SkillCountDTO;
import com.example.offerbrowserprototype.infrastructure.repository.OfferRepository;
import com.example.offerbrowserprototype.infrastructure.repository.RequirementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Testy jednostkowe handlera analityki wymagań rynkowych
@ExtendWith(MockitoExtension.class)
class RequirementAnalyticsHandlerTest {

    @Mock private RequirementRepository requirementRepository;
    @Mock private OfferRepository offerRepository;
    @InjectMocks private RequirementAnalyticsHandler handler;

    // Pomocnicza metoda tworząca mockową projekcję — unika tworzenia osobnych klas
    private SkillCountProjection proj(String skill, long count) {
        return new SkillCountProjection() {
            public String getSkill() { return skill; }
            public Long   getCount() { return count; }
        };
    }

    @Test
    void zwraca_top_skille_bez_filtra_gdy_sources_puste() {
        // given
        when(requirementRepository.findTopSkills(Pageable.unpaged()))
                .thenReturn(List.of(proj("Java", 10), proj("Spring", 5)));

        // when
        List<SkillCountDTO> result = handler.getTopSkills(List.of());

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSkill()).isEqualTo("Java");
        assertThat(result.get(0).getCount()).isEqualTo(10);
    }

    @Test
    void filtruje_skille_po_podanym_zrodle() {
        // given — filtr tylko dla NoFluff
        List<String> sources = List.of("NoFluff");
        when(requirementRepository.findTopSkillsBySources(sources, Pageable.unpaged()))
                .thenReturn(List.of(proj("Docker", 3)));

        // when
        List<SkillCountDTO> result = handler.getTopSkills(sources);

        // then — powinno użyć zapytania z filtrem źródeł
        assertThat(result).hasSize(1);
        verify(requirementRepository).findTopSkillsBySources(sources, Pageable.unpaged());
    }

    @Test
    void zwraca_skille_dla_zwyklego_poziomu() {
        // given
        when(requirementRepository.findTopSkillsByLevel("Junior", Pageable.unpaged()))
                .thenReturn(List.of(proj("Git", 4)));

        // when
        List<SkillCountDTO> result = handler.getTopSkillsByLevel("Junior", List.of());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkill()).isEqualTo("Git");
    }

    @Test
    void zwraca_skille_dla_nieokreslony_poziom_przez_zapytanie_null() {
        // given — "Nieokreślony poziom" to sentinel dla wartości NULL w bazie
        when(requirementRepository.findTopSkillsByNullLevel(Pageable.unpaged()))
                .thenReturn(List.of(proj("Linux", 2)));

        // when
        List<SkillCountDTO> result = handler.getTopSkillsByLevel(
                RequirementAnalyticsHandler.UNKNOWN_LEVEL, List.of());

        // then — powinno być przekierowane do zapytania IS NULL
        assertThat(result).hasSize(1);
        verify(requirementRepository).findTopSkillsByNullLevel(Pageable.unpaged());
    }

    @Test
    void dodaje_nieokreslony_poziom_gdy_istnieja_wiersze_z_null_poziomem() {
        // given
        when(requirementRepository.findDistinctLevels()).thenReturn(List.of("Junior", "Senior"));
        when(requirementRepository.existsNullLevel()).thenReturn(true);

        // when
        List<String> levels = handler.getAvailableLevels();

        // then — sentinel na końcu listy
        assertThat(levels).containsExactly("Junior", "Senior",
                RequirementAnalyticsHandler.UNKNOWN_LEVEL);
    }

    @Test
    void nie_dodaje_nieokreslony_poziom_gdy_brak_null_wierszy() {
        // given
        when(requirementRepository.findDistinctLevels()).thenReturn(List.of("Mid"));
        when(requirementRepository.existsNullLevel()).thenReturn(false);

        // when
        List<String> levels = handler.getAvailableLevels();

        // then
        assertThat(levels).containsExactly("Mid");
    }

    @Test
    void zwraca_dostepne_zrodla_z_tabeli_ofert() {
        // given — źródła brane z job_offers, nie z requirements
        when(offerRepository.findDistinctSources()).thenReturn(List.of("NoFluff", "Pracuj"));

        // when
        List<String> sources = handler.getAvailableSources();

        // then
        assertThat(sources).containsExactly("NoFluff", "Pracuj");
    }
}
