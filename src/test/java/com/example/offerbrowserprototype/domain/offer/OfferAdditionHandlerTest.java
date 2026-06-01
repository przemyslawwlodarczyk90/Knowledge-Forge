package com.example.offerbrowserprototype.domain.offer;

import com.example.offerbrowserprototype.domain.dto.offer.OfferDTO;
import com.example.offerbrowserprototype.domain.mapper.OfferMapper;
import com.example.offerbrowserprototype.domain.requirement.NiceToHaveCreationHandler;
import com.example.offerbrowserprototype.domain.requirement.RequirementCreationHandler;
import com.example.offerbrowserprototype.infrastructure.repository.OfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Testy jednostkowe handlera dodawania nowej oferty pracy
@ExtendWith(MockitoExtension.class)
class OfferAdditionHandlerTest {

    @Mock private OfferRepository offerRepository;
    @Mock private OfferMapper offerMapper;
    @Mock private RequirementCreationHandler requirementCreationHandler;
    @Mock private NiceToHaveCreationHandler niceToHaveCreationHandler;

    // Stały zegar — zapewnia powtarzalność testów zależnych od czasu
    private final Clock clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("UTC"));

    private OfferAdditionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OfferAdditionHandler(
                offerRepository, offerMapper, clock,
                requirementCreationHandler, niceToHaveCreationHandler);
    }

    @Test
    void dodaje_oferte_i_zwraca_zmapowane_dto() {
        // given
        OfferDTO inputDto = noweDto();
        Offer entity = Offer.builder().title("Java Dev").company("Firma").build();
        Offer saved  = Offer.builder().id(1L).title("Java Dev").company("Firma").build();
        OfferDTO expected = new OfferDTO(1L, "Java Dev", "Opis", "Warszawa",
                "http://url", "5000", "Firma", "Junior", false, LocalDateTime.now(clock));

        when(offerMapper.toEntity(inputDto)).thenReturn(entity);
        when(offerRepository.save(entity)).thenReturn(saved);
        when(offerMapper.toDTO(saved)).thenReturn(expected);

        // when
        OfferDTO result = handler.addOffer(inputDto);

        // then — wynik powinien być tym co zwrócił mapper po zapisaniu
        assertThat(result).isEqualTo(expected);
        verify(offerRepository).save(entity);
    }

    @Test
    void ustawia_fetchedAt_na_czas_z_zegara() {
        // given
        OfferDTO inputDto = noweDto();
        Offer entity = Offer.builder().title("T").company("C").build();
        Offer saved  = Offer.builder().id(1L).title("T").company("C").build();
        when(offerMapper.toEntity(inputDto)).thenReturn(entity);
        when(offerRepository.save(entity)).thenReturn(saved);
        when(offerMapper.toDTO(saved)).thenReturn(inputDto);

        // when
        handler.addOffer(inputDto);

        // then — czas pobrania oferty ustawiany przez handler, nie przez klienta
        assertThat(entity.getFetchedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void wywoluje_oba_handlery_wymagan_po_zapisaniu_oferty() {
        // given
        OfferDTO inputDto = noweDto();
        Offer entity = Offer.builder().title("T").company("C").build();
        Offer saved  = Offer.builder().id(1L).title("T").company("C").build();
        when(offerMapper.toEntity(inputDto)).thenReturn(entity);
        when(offerRepository.save(entity)).thenReturn(saved);
        when(offerMapper.toDTO(saved)).thenReturn(inputDto);

        // when
        handler.addOffer(inputDto);

        // then — oba handlery muszą być wywołane ze ZAPISANĄ ofertą (z id)
        verify(requirementCreationHandler).createFromOffer(saved);
        verify(niceToHaveCreationHandler).createFromOffer(saved);
    }

    private OfferDTO noweDto() {
        return new OfferDTO("Java Dev", "Opis", "Warszawa",
                "http://url", "5000", "Firma", "Junior", false, null);
    }
}
