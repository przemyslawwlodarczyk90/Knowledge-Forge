package com.example.offerbrowserprototype.domain.offer;

import com.example.offerbrowserprototype.infrastructure.repository.OfferFlagRepository;
import com.example.offerbrowserprototype.infrastructure.repository.OfferRepository;
import com.example.offerbrowserprototype.infrastructure.repository.UserOfferStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;

// Testy jednostkowe handlera usuwania oferty wraz z zależnymi danymi
@ExtendWith(MockitoExtension.class)
class OfferDeletionHandlerTest {

    @Mock private OfferRepository offerRepository;
    @Mock private UserOfferStatusRepository userOfferStatusRepository;
    @Mock private OfferFlagRepository offerFlagRepository;

    @InjectMocks
    private OfferDeletionHandler handler;

    @Test
    void usuwa_statusy_flagi_i_oferte_w_odpowiedniej_kolejnosci() {
        // when — usunięcie oferty powinno najpierw wyczyścić powiązane rekordy
        handler.deleteOffer(5L);

        // then — kolejność operacji ważna: FK statusy → flagi → oferta
        var order = inOrder(userOfferStatusRepository, offerFlagRepository, offerRepository);
        order.verify(userOfferStatusRepository).deleteByOffer_Id(5L);
        order.verify(offerFlagRepository).deleteByOffer_Id(5L);
        order.verify(offerRepository).deleteById(5L);
    }

    @Test
    void usuwa_z_poprawnym_id_oferty() {
        // when
        handler.deleteOffer(42L);

        // then — każda operacja wykonana dokładnie raz z właściwym ID
        inOrder(userOfferStatusRepository, offerFlagRepository, offerRepository)
                .verify(offerRepository).deleteById(42L);
    }
}
