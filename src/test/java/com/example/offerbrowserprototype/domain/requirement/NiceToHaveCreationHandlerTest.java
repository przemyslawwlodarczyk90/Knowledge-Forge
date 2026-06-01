//package com.example.offerbrowserprototype.domain.requirement;
//
//import com.example.offerbrowserprototype.domain.offer.Offer;
//import com.example.offerbrowserprototype.infrastructure.repository.NiceToHaveRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.argThat;
//import static org.mockito.Mockito.*;
//
//// Testy jednostkowe handlera tworzącego wpisy "mile widziane" z oferty
//@ExtendWith(MockitoExtension.class)
//class NiceToHaveCreationHandlerTest {
//
//    @Mock private NiceToHaveRepository niceToHaveRepository;
//    @InjectMocks private NiceToHaveCreationHandler handler;
//
//    @Test
//    void tworzy_wpis_dla_kazdego_skilla_mile_widzianego() {
//        // given
//        Offer offer = Offer.builder()
//                .niceToHave(List.of("Kotlin", "AWS"))
//                .source("NoFluff").level("Senior").build();
//
//        // when
//        handler.createFromOffer(offer);
//
//        // then — dwa wpisy: Kotlin i AWS
//        verify(niceToHaveRepository, times(2)).save(any(NiceToHave.class));
//        verify(niceToHaveRepository).save(argThat(n -> n.getSkill().equals("Kotlin")));
//        verify(niceToHaveRepository).save(argThat(n -> n.getSkill().equals("AWS")));
//    }
//
//    @Test
//    void przypisuje_source_i_level_z_oferty() {
//        // given
//        Offer offer = Offer.builder()
//                .niceToHave(List.of("TypeScript"))
//                .source("NoFluff").level("Junior").build();
//
//        // when
//        handler.createFromOffer(offer);
//
//        // then
//        verify(niceToHaveRepository).save(argThat(n ->
//                "NoFluff".equals(n.getSource()) && "Junior".equals(n.getLevel())));
//    }
//
//    @Test
//    void nie_tworzy_wpisow_gdy_nice_to_have_jest_null() {
//        // given
//        Offer offer = Offer.builder().niceToHave(null).build();
//
//        // when
//        handler.createFromOffer(offer);
//
//        // then
//        verifyNoInteractions(niceToHaveRepository);
//    }
//
//    @Test
//    void nie_tworzy_wpisow_gdy_lista_jest_pusta() {
//        // given
//        Offer offer = Offer.builder().niceToHave(List.of()).build();
//
//        // when
//        handler.createFromOffer(offer);
//
//        // then
//        verifyNoInteractions(niceToHaveRepository);
//    }
//}
