package com.north.producoes.controller;

import com.north.producoes.controller.api.FinanceApi;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.dto.response.FinanceResponse;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.service.FinanceService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class FinanceController implements FinanceApi {

    private final FinanceService financeService;

    @Override
    public ResponseEntity<List<FinanceResponse>> findAll() {
        List<FinanceResponse> finance = financeService.findAll()
                .stream()
                .map(FinanceResponse::from)
                .toList();
        return ResponseEntity.ok(finance);
    }

    @Override
    public ResponseEntity<List<FinanceResponse>> findByStatus(FinanceStatusEnum status) {
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        List<FinanceResponse> financeStatus = financeService.findByStatus(status)
                .stream()
                .map(FinanceResponse::from)
                .toList();
        return ResponseEntity.ok(financeStatus);
    }

    @Override
    public ResponseEntity<List<FinanceResponse>> findByClient(Long id){
        List<FinanceResponse> financeClient = financeService.findByClientId(id)
                .stream()
                .map(FinanceResponse::from)
                .toList();
        return ResponseEntity.ok(financeClient);
    }

    @Override
    public ResponseEntity<FinanceResponse> saveFinance(FinanceEntity finance) {
        return ResponseEntity.ok(FinanceResponse.from(financeService.saveFinance(finance)));
    }

    @Override
    public ResponseEntity<FinanceResponse> updateFinance(FinanceEntity finance) {
        return ResponseEntity.ok(FinanceResponse.from(financeService.updateFinance(finance)));
    }

    @Override
    public ResponseEntity<Void> deleteFinanceById(Long id) {
        financeService.deleteFinanceById(id);
        return ResponseEntity.noContent().build();
    }
}
