package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.FinanceRequestDTO;
import com.north.producoes.controller.dto.response.FinanceResponseDTO;
import com.north.producoes.controller.dto.response.ForecastResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.entity.enums.FinanceTypeEnum;
import com.north.producoes.service.FinanceService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
@AllArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/forecast")
    public ResponseEntity<ForecastResponseDTO> getForecast(
            @RequestParam(required = false) Integer year) {
        int targetYear = (year != null && year > 0) ? year : LocalDateTime.now().getYear();
        return ResponseEntity.ok(financeService.getForecast(targetYear));
    }

    @GetMapping
    public ResponseEntity<List<FinanceResponseDTO>> findAll() {
        return ResponseEntity.ok(
                financeService.findAll().stream().map(FinanceResponseDTO::from).toList()
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<FinanceResponseDTO>> findByStatus(@PathVariable FinanceStatusEnum status) {
        return ResponseEntity.ok(
                financeService.findByStatus(status).stream().map(FinanceResponseDTO::from).toList()
        );
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<FinanceResponseDTO>> findByType(@PathVariable FinanceTypeEnum type) {
        return ResponseEntity.ok(
                financeService.findByType(type).stream().map(FinanceResponseDTO::from).toList()
        );
    }

    @GetMapping("/client/{id}")
    public ResponseEntity<List<FinanceResponseDTO>> findByClient(@PathVariable Long id) {
        return ResponseEntity.ok(
                financeService.findByClientId(id).stream().map(FinanceResponseDTO::from).toList()
        );
    }

    @PostMapping
    public ResponseEntity<FinanceResponseDTO> saveFinance(
            @Valid @RequestBody FinanceRequestDTO request,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FinanceResponseDTO.from(financeService.saveFinance(request, currentUser)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinanceResponseDTO> updateFinance(
            @PathVariable Long id,
            @Valid @RequestBody FinanceRequestDTO request) {
        return ResponseEntity.ok(FinanceResponseDTO.from(financeService.updateFinance(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFinanceById(@PathVariable Long id) {
        financeService.deleteFinanceById(id);
        return ResponseEntity.noContent().build();
    }
}
