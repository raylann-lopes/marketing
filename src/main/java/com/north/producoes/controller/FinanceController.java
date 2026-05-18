package com.north.producoes.controller;

import com.north.producoes.controller.dto.response.FinanceResponseDTO;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.service.FinanceService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@AllArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping
    public ResponseEntity<List<FinanceResponseDTO>> findAll() {
        List<FinanceResponseDTO> finance = financeService.findAll()
                .stream()
                .map(FinanceResponseDTO::from)
                .toList();
        return ResponseEntity.ok(finance);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<FinanceResponseDTO>> findByStatus(@PathVariable FinanceStatusEnum status) {
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        List<FinanceResponseDTO> financeStatus = financeService.findByStatus(status)
                .stream()
                .map(FinanceResponseDTO::from)
                .toList();
        return ResponseEntity.ok(financeStatus);
    }

    @GetMapping("/client/{id}")
    public ResponseEntity<List<FinanceResponseDTO>> findByClient(@PathVariable Long id){
        List<FinanceResponseDTO> financeClient = financeService.findByClientId(id)
                .stream()
                .map(FinanceResponseDTO::from)
                .toList();
        return ResponseEntity.ok(financeClient);
    }

    @PostMapping("/create")
    public ResponseEntity<FinanceResponseDTO> saveFinance(@Valid @RequestBody FinanceEntity finance) {
        return ResponseEntity.ok(FinanceResponseDTO.from(financeService.saveFinance(finance)));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<FinanceResponseDTO> updateFinance(@PathVariable Long id, @Valid @RequestBody FinanceEntity finance) {
        return ResponseEntity.ok(FinanceResponseDTO.from(financeService.updateFinance(id, finance)));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteFinanceById(@PathVariable Long id) {
        financeService.deleteFinanceById(id);
        return ResponseEntity.noContent().build();
    }
}
