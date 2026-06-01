package com.example.offerbrowserprototype.domain.usseroffer;

import com.example.offerbrowserprototype.domain.aplicationnote.ApplicationNoteHandler;
import com.example.offerbrowserprototype.domain.exception.OfferAlreadyAppliedException;
import com.example.offerbrowserprototype.domain.offer.Offer;
import com.example.offerbrowserprototype.domain.user.User;
import com.example.offerbrowserprototype.infrastructure.repository.OfferRepository;
import com.example.offerbrowserprototype.infrastructure.repository.UserOfferStatusRepository;
import com.example.offerbrowserprototype.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Testy jednostkowe handlera aplikowania na ofertę
@ExtendWith(MockitoExtension.class)
class UserOfferApplyHandlerTest {

    @Mock private UserOfferStatusRepository userOfferStatusRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationNoteHandler applicationNoteHandler;
    @InjectMocks private UserOfferApplyHandler handler;

    @Test
    void zapisuje_aplikacje_i_ustawia_applied_true() {
        // given
        User user = new User();
        user.setId(1L);
        Offer offer = Offer.builder().id(10L).offerUrl("http://url").company("Firma").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(offerRepository.findById(10L)).thenReturn(Optional.of(offer));
        when(userOfferStatusRepository.findByUserAndOffer(user, offer)).thenReturn(Optional.empty());
        when(userOfferStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        UserOfferStatus result = handler.applyToOffer(1L, 10L);

        // then — status powinien być zaznaczony jako zaaplikowany
        assertThat(result.isApplied()).isTrue();
        assertThat(result.getAppliedAt()).isNotNull();
    }

    @Test
    void rzuca_wyjatek_gdy_uzytkownik_juz_aplikowal_na_te_oferte() {
        // given — status already applied=true
        User user = new User();
        user.setId(1L);
        Offer offer = Offer.builder().id(10L).offerUrl("http://url").company("Firma").build();
        UserOfferStatus existing = new UserOfferStatus(user, offer, true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(offerRepository.findById(10L)).thenReturn(Optional.of(offer));
        when(userOfferStatusRepository.findByUserAndOffer(user, offer))
                .thenReturn(Optional.of(existing));

        // when / then
        assertThatThrownBy(() -> handler.applyToOffer(1L, 10L))
                .isInstanceOf(OfferAlreadyAppliedException.class);
    }

    @Test
    void rzuca_wyjatek_gdy_uzytkownik_nie_istnieje_w_bazie() {
        // given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> handler.applyToOffer(99L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void rzuca_wyjatek_gdy_oferta_nie_istnieje_w_bazie() {
        // given
        User user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(offerRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> handler.applyToOffer(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Offer not found");
    }
}
