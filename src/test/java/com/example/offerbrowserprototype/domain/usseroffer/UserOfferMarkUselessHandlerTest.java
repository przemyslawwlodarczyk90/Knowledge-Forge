package com.example.offerbrowserprototype.domain.usseroffer;

import com.example.offerbrowserprototype.domain.offer.FlagType;
import com.example.offerbrowserprototype.domain.offer.OfferFlagHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

// Testy jednostkowe handlera oznaczania oferty jako nieprzydatna przez użytkownika
@ExtendWith(MockitoExtension.class)
class UserOfferMarkUselessHandlerTest {

    @Mock private OfferFlagHandler offerFlagHandler;
    @InjectMocks private UserOfferMarkUselessHandler handler;

    @Test
    void deleguje_do_offer_flag_handlera_z_typem_useless() {
        // when
        handler.markAsUseless(1L, 10L);

        // then — handler deleguje do flagowania z właściwym typem
        verify(offerFlagHandler).flag(1L, 10L, FlagType.USELESS);
    }

    @Test
    void przekazuje_poprawne_id_uzytkownika_i_oferty() {
        // when
        handler.markAsUseless(7L, 42L);

        // then
        verify(offerFlagHandler).flag(7L, 42L, FlagType.USELESS);
    }
}
