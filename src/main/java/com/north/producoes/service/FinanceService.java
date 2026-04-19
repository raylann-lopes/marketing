package com.north.producoes.service;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.FinanceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class FinanceService {
    private final FinanceRepository financeRepository;

    public List<FinanceEntity> findAll(){
        return financeRepository.findAll();
    }

    public List<FinanceEntity> findByStatus(FinanceStatusEnum status){
        List<FinanceEntity> financeStatus = financeRepository.findByStatus(status);
        if (financeStatus.isEmpty()){
            throw new ResourceNotFoundException("Conta com status" + status + "nao encontrada!");
        }
        return financeStatus;
    }

    public List<FinanceEntity> findByClientId(Long id){
        List<FinanceEntity> financeClient = financeRepository.findByClientId(id);
        if (financeClient.isEmpty()){
            throw new ResourceNotFoundException("Conta do cliente nao encontrada: id " + id);
        }
        return financeClient;
    }

    public FinanceEntity saveFinance(FinanceEntity finance){
        return financeRepository.save(finance);
    }

    public FinanceEntity updateFinance(Long id, FinanceEntity finance){
        if (!financeRepository.existsById(finance.getId())){
            throw new ResourceNotFoundException("Conta nao encontrada: id " + finance.getId());
        }
        finance.setId(id);
        return financeRepository.save(finance);
    }

    public void deleteFinanceById(Long id){
        if (!financeRepository.existsById(id)){
            throw new ResourceNotFoundException("Conta nao encontrada: id " + id);
        }
        financeRepository.deleteById(id);
    }
}
