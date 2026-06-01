package com.example.offerbrowserprototype.domain.usseroffer;

import com.example.offerbrowserprototype.domain.offer.FlagType;
import com.example.offerbrowserprototype.domain.offer.Offer;
import com.example.offerbrowserprototype.infrastructure.repository.OfferFlagRepository;
import com.example.offerbrowserprototype.infrastructure.repository.OfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Testy jednostkowe handlera pobierającego oferty oznaczone przez użytkownika jako nieprzydatne
@ExtendWith(MockitoExtension.class)
class UserOfferGetUselessHandlerTest {

    @Mock private OfferFlagRepository offerFlagRepository;
    @Mock private OfferRepository offerRepository;
    @InjectMocks private UserOfferGetUselessHandler handler;

    @Test
    void zwraca_oferty_uzytkownika_oznaczone_jako_nieprzydatne() {
        // given
        Long userId = 1L;
        List<Long> uselessIds = List.of(5L, 7L);
        List<Offer> expected = List.of(
                Offer.builder().id(5L).title("A").company("FA").build(),
                Offer.builder().id(7L).title("B").company("FB").build());

        when(offerFlagRepository.findOfferIdsByUserIdAndType(userId, FlagType.USELESS))
                .thenReturn(uselessIds);
        when(offerRepository.findAllById(uselessIds)).thenReturn(expected);

        // when
        List<Offer> result = handler.getUselessOffersForUser(userId);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void zwraca_pusta_liste_i_nie_pyta_repo_ofert_gdy_brak_nieprzydatnych() {
        // given
        when(offerFlagRepository.findOfferIdsByUserIdAndType(1L, FlagType.USELESS))
                .thenReturn(List.of());

        // when
        List<Offer> result = handler.getUselessOffersForUser(1L);

        // then — shortcut: nie odpytujemy repozytorium ofert gdy lista ID jest pusta
        assertThat(result).isEmpty();
        verifyNoInteractions(offerRepository);
    }
}
