package com.north.producoes.controller;

import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.dto.response.FinanceResponse;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.service.FinanceService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/finance")
public class FinanceController {
    private final FinanceService financeService;

    @GetMapping
    public ResponseEntity<List<FinanceResponse>> findAll(){
        List<FinanceResponse> finance = financeService.findAll()
                .stream()
                .map(FinanceResponse::from)
                .toList();
        return ResponseEntity.ok(finance);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<FinanceResponse>> findByStatus(FinanceStatusEnum status){
        List<FinanceResponse> financeStatus = financeService.findByStatus(status)
                .stream()
                .map(FinanceResponse::from)
                .toList();
        return ResponseEntity.ok(financeStatus);
    }

    @PostMapping("/create")
    public ResponseEntity<FinanceResponse> saveFinance(FinanceEntity finance){
        FinanceResponse financeResponse = FinanceResponse.from(financeService.saveFinance(finance));
        return ResponseEntity.ok(financeResponse);
    }

    @PostMapping("/update")
    public ResponseEntity<FinanceResponse> updateFinance(FinanceEntity finance){
        FinanceResponse financeResponse = FinanceResponse.from(financeService.updateFinance(finance));
        return ResponseEntity.ok(financeResponse);
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deleteFinanceById(Long id){
        financeService.deleteFinanceById(id);
        return ResponseEntity.noContent().build();
    }
}
