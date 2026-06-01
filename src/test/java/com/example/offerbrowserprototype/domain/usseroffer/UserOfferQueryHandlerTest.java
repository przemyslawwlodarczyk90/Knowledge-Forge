package com.example.offerbrowserprototype.domain.usseroffer;

import com.example.offerbrowserprototype.domain.offer.FlagType;
import com.example.offerbrowserprototype.domain.offer.Offer;
import com.example.offerbrowserprototype.infrastructure.repository.OfferFlagRepository;
import com.example.offerbrowserprototype.infrastructure.repository.OfferRepository;
import com.example.offerbrowserprototype.infrastructure.repository.UserOfferStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Testy jednostkowe handlera pobierającego oferty niezaaplikowane przez użytkownika
@ExtendWith(MockitoExtension.class)
class UserOfferQueryHandlerTest {

    @Mock private OfferRepository offerRepository;
    @Mock private UserOfferStatusRepository userOfferStatusRepository;
    @Mock private OfferFlagRepository offerFlagRepository;

    @InjectMocks
    private UserOfferQueryHandler handler;

    @Test
    void zwraca_wszystkie_oferty_gdy_brak_wykluczonych() {
        // given — użytkownik niczego nie ukrył ani nie zaaplikował
        Long userId = 1L;
        when(userOfferStatusRepository.findByUser_IdAndAppliedTrue(userId)).thenReturn(List.of());
        when(offerFlagRepository.findOfferIdsByUserIdAndType(userId, FlagType.USELESS)).thenReturn(List.of());
        when(offerFlagRepository.findOfferIdsByUserIdAndType(userId, FlagType.DUPLICATE)).thenReturn(List.of());

        Offer offer = Offer.builder().id(1L).title("A").company("FA").build();
        when(offerRepository.findAllByOrderByFetchedAtDesc()).thenReturn(List.of(offer));

        // when
        List<Offer> result = handler.getNotAppliedOffersForUser(userId);

        // then — cała lista dostępna
        assertThat(result).containsExactly(offer);
    }

    @Test
    void wyklucza_oferty_do_ktorych_uzytkownik_juz_apliko() {
        // given — oferta ID=1 jest już zaaplikowana
        Long userId = 2L;
        UserOfferStatus applied = new UserOfferStatus();
        applied.setOffer(Offer.builder().id(1L).company("C").build());
        applied.setApplied(true);

        when(userOfferStatusRepository.findByUser_IdAndAppliedTrue(userId))
                .thenReturn(List.of(applied));
        when(offerFlagRepository.findOfferIdsByUserIdAndType(userId, FlagType.USELESS))
                .thenReturn(List.of());
        when(offerFlagRepository.findOfferIdsByUserIdAndType(userId, FlagType.DUPLICATE))
                .thenReturn(List.of());

        Offer o2 = Offer.builder().id(2L).title("B").company("FB").build();
        when(offerRepository.findByIdNotIn(List.of(1L))).thenReturn(List.of(o2));

        // when
        List<Offer> result = handler.getNotAppliedOffersForUser(userId);

        // then — tylko oferta ID=2 widoczna
        assertThat(result).containsExactly(o2);
    }

    @Test
    void wyklucza_oferty_oznaczone_jako_nieprzydatne() {
        // given — oferta ID=5 oznaczona jako nieprzydatna
        Long userId = 3L;
        when(userOfferStatusRepository.findByUser_IdAndAppliedTrue(userId)).thenReturn(List.of());
        when(offerFlagRepository.findOfferIdsByUserIdAndType(userId, FlagType.USELESS))
                .thenReturn(List.of(5L));
        when(offerFlagRepository.findOfferIdsByUserIdAndType(userId, FlagType.DUPLICATE))
                .thenReturn(List.of());

        Offer other = Offer.builder().id(10L).title("C").company("FC").build();
        when(offerRepository.findByIdNotIn(List.of(5L))).thenReturn(List.of(other));

        // when
        List<Offer> result = handler.getNotAppliedOffersForUser(userId);

        // then
        assertThat(result).containsExactly(other);
    }
}
