package com.jupjup.Backend.domain.review;

import com.jupjup.Backend.domain.review.dto.ReportCreateRequest;
import com.jupjup.Backend.domain.review.dto.ReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 신고 접수
    @PostMapping
    public ResponseEntity<ReportResponse> create(
            @Valid @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(reportService.create(request, email));
    }
}