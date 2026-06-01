package com.example.offerbrowserprototype.infrastructure.web;

import com.example.offerbrowserprototype.domain.progress.dto.ProgressSummaryDto;
import com.example.offerbrowserprototype.infrastructure.facade.ProgressFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressFacade progressFacade;

    @GetMapping("/summary")
    public ResponseEntity<ProgressSummaryDto> getSummary() {
        return ResponseEntity.ok(progressFacade.getSummary());
    }
}
