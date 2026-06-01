package com.example.offerbrowserprototype.infrastructure.facade;

import com.example.offerbrowserprototype.domain.dto.useroffer.UserOfferStatusDTO;
import com.example.offerbrowserprototype.domain.mapper.OfferMapper;
import com.example.offerbrowserprototype.domain.mapper.UserOfferStatusMapper;
import com.example.offerbrowserprototype.domain.usseroffer.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Testy jednostkowe fasady operacji użytkownika na ofertach
@ExtendWith(MockitoExtension.class)
class UserOfferFacadeTest {

    @Mock private UserOfferApplyHandler applyHandler;
    @Mock private UserOfferMarkUselessHandler markUselessHandler;
    @Mock private UserOfferGetUselessHandler getUselessHandler;
    @Mock private UserOfferStatusMapper statusMapper;
    @Mock private OfferMapper offerMapper;
    @Mock private UserOfferQueryHandler queryHandler;
    @Mock private UserAppliedOffersHandler appliedOffersHandler;
    @Mock private UserAppliedOffersCountHandler countHandler;
    @InjectMocks private UserOfferFacade facade;

    @Test
    void applyToOffer_deleguje_i_zwraca_dto_statusu() {
        // given
        UserOfferStatus status = new UserOfferStatus();
        UserOfferStatusDTO dto = new UserOfferStatusDTO();
        when(applyHandler.applyToOffer(1L, 10L)).thenReturn(status);
        when(statusMapper.toDTO(status)).thenReturn(dto);

        // when / then
        assertThat(facade.applyToOffer(1L, 10L)).isEqualTo(dto);
    }

    @Test
    void markAsUseless_deleguje_do_handlera() {
        // when
        facade.markAsUseless(1L, 5L);

        // then
        verify(markUselessHandler).markAsUseless(1L, 5L);
    }

    @Test
    void countAppliedOffersForUser_deleguje_i_zwraca_liczbe() {
        // given
        when(countHandler.countAppliedOffersForUser(1L)).thenReturn(3L);

        // when / then
        assertThat(facade.countAppliedOffersForUser(1L)).isEqualTo(3L);
    }
}
