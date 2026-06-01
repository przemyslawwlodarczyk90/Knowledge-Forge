package com.example.offerbrowserprototype.domain.requirement;

import com.example.offerbrowserprototype.domain.offer.Offer;
import com.example.offerbrowserprototype.infrastructure.repository.RequirementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

// Testy jednostkowe handlera tworzącego wpisy statystyczne wymagań z oferty
@ExtendWith(MockitoExtension.class)
class RequirementCreationHandlerTest {

    @Mock private RequirementRepository requirementRepository;
    @InjectMocks private RequirementCreationHandler handler;

    @Test
    void tworzy_wpis_dla_kazdego_skilla_z_listy_wymagan() {
        // given — oferta z dwoma wymaganiami technicznymi
        Offer offer = Offer.builder()
                .requirements(List.of("Java", "Spring"))
                .source("NoFluff").level("Junior").build();

        // when
        handler.createFromOffer(offer);

        // then — dwa wpisy powinny trafić do bazy
        verify(requirementRepository, times(2)).save(any(Requirement.class));
        verify(requirementRepository).save(argThat(r -> r.getSkill().equals("Java")));
        verify(requirementRepository).save(argThat(r -> r.getSkill().equals("Spring")));
    }

    @Test
    void przypisuje_source_i_level_z_oferty_do_kazdego_wymagania() {
        // given
        Offer offer = Offer.builder()
                .requirements(List.of("Docker"))
                .source("Pracuj").level("Mid").build();

        // when
        handler.createFromOffer(offer);

        // then — wpis powinien odzwierciedlać źródło i poziom oferty
        verify(requirementRepository).save(argThat(r ->
                "Pracuj".equals(r.getSource()) && "Mid".equals(r.getLevel())));
    }

    @Test
    void nie_tworzy_wpisow_gdy_lista_wymagan_jest_null() {
        // given — oferta bez sekcji wymagań
        Offer offer = Offer.builder().requirements(null).build();

        // when
        handler.createFromOffer(offer);

        // then — brak zapisu do bazy
        verifyNoInteractions(requirementRepository);
    }

    @Test
    void nie_tworzy_wpisow_gdy_lista_wymagan_jest_pusta() {
        // given
        Offer offer = Offer.builder().requirements(List.of()).build();

        // when
        handler.createFromOffer(offer);

        // then
        verifyNoInteractions(requirementRepository);
    }
}
