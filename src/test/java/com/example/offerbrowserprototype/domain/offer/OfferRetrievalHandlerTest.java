package com.example.offerbrowserprototype.domain.offer;

import com.example.offerbrowserprototype.domain.dto.offer.OfferDTO;
import com.example.offerbrowserprototype.domain.mapper.OfferMapper;
import com.example.offerbrowserprototype.infrastructure.repository.OfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Testy jednostkowe handlera pobierania ofert z bazy danych
@ExtendWith(MockitoExtension.class)
class OfferRetrievalHandlerTest {

    @Mock private OfferRepository offerRepository;
    @Mock private OfferMapper offerMapper;

    @InjectMocks
    private OfferRetrievalHandler handler;

    @Test
    void zwraca_dto_dla_istniejacego_id() {
        // given
        Offer offer = Offer.builder().id(1L).title("Java Dev").company("Firma").build();
        OfferDTO dto  = new OfferDTO(1L, "Java Dev", "D", "L", "U", "S", "Firma", "Mid", false, null);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(offerMapper.toDTO(offer)).thenReturn(dto);

        // when
        OfferDTO result = handler.getOffer(1L);

        // then
        assertThat(result).isEqualTo(dto);
    }

    @Test
    void zwraca_null_gdy_oferta_nie_istnieje() {
        // given — oferta o podanym ID nie ma w bazie
        when(offerRepository.findById(99L)).thenReturn(Optional.empty());

        // when
        OfferDTO result = handler.getOffer(99L);

        // then
        assertThat(result).isNull();
    }

    @Test
    void zwraca_liste_wszystkich_ofert_posortowanych() {
        // given
        Offer o1 = Offer.builder().id(1L).title("A").company("FA").build();
        Offer o2 = Offer.builder().id(2L).title("B").company("FB").build();
        OfferDTO d1 = new OfferDTO(1L, "A", "D", "L", "U1", "S", "FA", "J", false, null);
        OfferDTO d2 = new OfferDTO(2L, "B", "D", "L", "U2", "S", "FB", "M", false, null);

        when(offerRepository.findAllByOrderByFetchedAtDesc()).thenReturn(List.of(o1, o2));
        when(offerMapper.toDTO(o1)).thenReturn(d1);
        when(offerMapper.toDTO(o2)).thenReturn(d2);

        // when
        List<OfferDTO> result = handler.getAllOffers();

        // then
        assertThat(result).containsExactly(d1, d2);
    }

    @Test
    void zwraca_pusta_liste_gdy_brak_ofert_w_bazie() {
        // given
        when(offerRepository.findAllByOrderByFetchedAtDesc()).thenReturn(List.of());

        // when
        List<OfferDTO> result = handler.getAllOffers();

        // then
        assertThat(result).isEmpty();
    }
}
