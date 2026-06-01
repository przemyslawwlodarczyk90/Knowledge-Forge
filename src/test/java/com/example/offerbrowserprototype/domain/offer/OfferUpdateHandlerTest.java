package com.example.offerbrowserprototype.domain.offer;

import com.example.offerbrowserprototype.domain.dto.offer.OfferDTO;
import com.example.offerbrowserprototype.domain.mapper.OfferMapper;
import com.example.offerbrowserprototype.infrastructure.repository.OfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// Testy jednostkowe handlera aktualizacji istniejącej oferty
@ExtendWith(MockitoExtension.class)
class OfferUpdateHandlerTest {

    @Mock private OfferRepository offerRepository;
    @Mock private OfferMapper offerMapper;

    private final Clock clock = Clock.fixed(Instant.parse("2024-06-01T08:00:00Z"), ZoneId.of("UTC"));
    private OfferUpdateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OfferUpdateHandler(offerRepository, offerMapper, clock);
    }

    @Test
    void aktualizuje_pola_oferty_i_zwraca_dto() {
        // given
        Offer existing = Offer.builder().id(1L).title("Stary").company("Firma").build();
        OfferDTO updateDto = new OfferDTO(1L, "Nowy", "Opis", "Kraków",
                "http://nowy", "6000", "NowaFirma", "Senior", false, null);
        OfferDTO expected  = new OfferDTO(1L, "Nowy", "Opis", "Kraków",
                "http://nowy", "6000", "NowaFirma", "Senior", false, null);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(existing)).thenReturn(existing);
        when(offerMapper.toDTO(existing)).thenReturn(expected);

        // when
        OfferDTO result = handler.updateOffer(1L, updateDto);

        // then — encja powinna zostać zmutowana i zwrócony nowy DTO
        assertThat(result).isEqualTo(expected);
        assertThat(existing.getTitle()).isEqualTo("Nowy");
        assertThat(existing.getLevel()).isEqualTo("Senior");
    }

    @Test
    void rzuca_wyjatek_gdy_oferta_o_podanym_id_nie_istnieje() {
        // given — brak oferty w bazie
        when(offerRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> handler.updateOffer(99L, new OfferDTO()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
