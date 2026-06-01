package com.example.offerbrowserprototype.infrastructure.facade;

import com.example.offerbrowserprototype.domain.statistics.CityDistributionHandler;
import com.example.offerbrowserprototype.domain.statistics.LevelDistributionHandler;
import com.example.offerbrowserprototype.domain.statistics.OfferCountHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Testy jednostkowe fasady statystyk ofert — delegacja do handlerów dziedzinowych
@ExtendWith(MockitoExtension.class)
class StatisticsFacadeTest {

    @Mock private OfferCountHandler offerCountHandler;
    @Mock private LevelDistributionHandler levelDistributionHandler;
    @Mock private CityDistributionHandler cityDistributionHandler;
    @InjectMocks private StatisticsFacade facade;

    @Test
    void getTotalOffers_deleguje_i_zwraca_liczbe() {
        // given
        when(offerCountHandler.getTotalOffers()).thenReturn(42L);

        // when / then
        assertThat(facade.getTotalOffers()).isEqualTo(42L);
    }

    @Test
    void getLevelDistribution_zwraca_mape_poziomow() {
        // given
        Map<String, Long> dist = Map.of("Junior", 10L, "Senior", 5L);
        when(levelDistributionHandler.getLevelDistribution()).thenReturn(dist);

        // when
        Map<String, Long> result = facade.getLevelDistribution();

        // then
        assertThat(result).containsEntry("Junior", 10L).containsEntry("Senior", 5L);
    }

    @Test
    void getCityDistribution_zwraca_mape_miast() {
        // given
        Map<String, Long> cities = Map.of("Warszawa", 20L, "Kraków", 8L);
        when(cityDistributionHandler.getCityDistribution()).thenReturn(cities);

        // when
        Map<String, Long> result = facade.getCityDistribution();

        // then
        assertThat(result).containsEntry("Warszawa", 20L);
    }
}
