package com.example.offerbrowserprototype.domain.mapper;

import com.example.offerbrowserprototype.domain.dto.offer.OfferDTO;
import com.example.offerbrowserprototype.domain.offer.Offer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Testy jednostkowe mappera ofert — konwersja encja ↔ DTO
class OfferMapperTest {

    // Mapper nie ma zależności — testujemy go bez mocków
    private final OfferMapper mapper = new OfferMapper();

    @Test
    void mapuje_wszystkie_pola_encji_do_dto() {
        // given
        Offer offer = Offer.builder()
                .id(1L).title("Java Dev").description("Opis").location("Warszawa")
                .offerUrl("http://url").salaryRange("5000").company("Firma")
                .level("Junior").fetchedAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .requirements(List.of("Java", "SQL"))
                .niceToHave(List.of("Kotlin"))
                .source("NoFluff").build();

        // when
        OfferDTO dto = mapper.toDTO(offer);

        // then — każde pole encji musi być odwzorowane
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Java Dev");
        assertThat(dto.getRequirements()).containsExactly("Java", "SQL");
        assertThat(dto.getNiceToHave()).containsExactly("Kotlin");
        assertThat(dto.getSource()).isEqualTo("NoFluff");
        assertThat(dto.getLevel()).isEqualTo("Junior");
    }

    @Test
    void mapuje_dto_do_encji() {
        // given
        OfferDTO dto = new OfferDTO("Backend Dev", "Opis", "Kraków",
                "http://url2", "7000", "SuperFirma", "Mid", false, null);
        dto.setRequirements(List.of("Spring Boot"));
        dto.setSource("Pracuj");

        // when
        Offer offer = mapper.toEntity(dto);

        // then
        assertThat(offer.getTitle()).isEqualTo("Backend Dev");
        assertThat(offer.getRequirements()).containsExactly("Spring Boot");
        assertThat(offer.getSource()).isEqualTo("Pracuj");
        assertThat(offer.getLevel()).isEqualTo("Mid");
    }

    @Test
    void requirements_w_dto_sa_niezalezna_kopia_listy() {
        // given — zapobiega trzymaniu referencji do Hibernate PersistentBag
        List<String> original = new ArrayList<>(List.of("A", "B"));
        Offer offer = Offer.builder().id(1L).title("T").company("C")
                .requirements(original).build();

        // when
        OfferDTO dto = mapper.toDTO(offer);
        original.add("C"); // modyfikacja oryginalnej listy po mapowaniu

        // then — DTO nie powinno być dotknięte zmianą
        assertThat(dto.getRequirements()).containsExactly("A", "B");
    }

    @Test
    void zwraca_null_dla_requirements_gdy_oferta_ich_nie_ma() {
        // given
        Offer offer = Offer.builder().id(1L).title("T").company("C")
                .requirements(null).niceToHave(null).build();

        // when
        OfferDTO dto = mapper.toDTO(offer);

        // then — null powinien pozostać nullem
        assertThat(dto.getRequirements()).isNull();
        assertThat(dto.getNiceToHave()).isNull();
    }
}
