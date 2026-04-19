package com.north.producoes.controller;

import com.north.producoes.controller.api.FinanceApi;
import com.north.producoes.controller.dto.response.FinanceResponseDTO;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.service.FinanceService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FinanceController implements FinanceApi {

    private final FinanceService financeService;

    @Override
    public ResponseEntity<List<FinanceResponseDTO>> findAll() {
        List<FinanceResponseDTO> finance = financeService.findAll()
                .stream()
                .map(FinanceResponseDTO::from)
                .toList();
        return ResponseEntity.ok(finance);
    }

    @Override
    public ResponseEntity<List<FinanceResponseDTO>> findByStatus(FinanceStatusEnum status) {
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        List<FinanceResponseDTO> financeStatus = financeService.findByStatus(status)
                .stream()
                .map(FinanceResponseDTO::from)
                .toList();
        return ResponseEntity.ok(financeStatus);
    }

    @Override
    public ResponseEntity<List<FinanceResponseDTO>> findByClient(Long id){
        List<FinanceResponseDTO> financeClient = financeService.findByClientId(id)
                .stream()
                .map(FinanceResponseDTO::from)
                .toList();
        return ResponseEntity.ok(financeClient);
    }

    @Override
    public ResponseEntity<FinanceResponseDTO> saveFinance(@Valid FinanceEntity finance) {
        return ResponseEntity.ok(FinanceResponseDTO.from(financeService.saveFinance(finance)));
    }

    @Override
    public ResponseEntity<FinanceResponseDTO> updateFinance(Long id, @Valid FinanceEntity finance) {
        return ResponseEntity.ok(FinanceResponseDTO.from(financeService.updateFinance(id, finance)));
    }

    @Override
    public ResponseEntity<Void> deleteFinanceById(Long id) {
        financeService.deleteFinanceById(id);
        return ResponseEntity.noContent().build();
    }
}
