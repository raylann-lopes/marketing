package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
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
            throw new RuntimeException("Conta nao encontrado");
        }
        return financeStatus;
    }

    public List<FinanceEntity> findByClient(ClientEntity clientId){
        List<FinanceEntity> financeClient = financeRepository.findByClientId(clientId);
        if (financeClient.isEmpty()){
            throw new RuntimeException("Conta nao encontrado");
        }
        return financeClient;
    }

    public List<FinanceEntity> findByUser(UserEntity userId){
        List<FinanceEntity> financeUser = financeRepository.findByUserId(userId);
        if (financeUser.isEmpty()){
            throw new RuntimeException("Conta nao encontrado");
        }
        return financeUser;
    }

    public FinanceEntity saveFinance(FinanceEntity finance){
        return financeRepository.save(finance);
    }

    public FinanceEntity updateFinance(FinanceEntity finance){
        if (financeRepository.findById(finance.getId()).isEmpty()){
            throw new RuntimeException("Conta nao encontrada");
        }
        return financeRepository.save(finance);
    }

    public void deleteFinanceById(Long id){
        if (financeRepository.findById(id).isEmpty()){
            throw new RuntimeException("Conta nao encontrada");
        }
        financeRepository.deleteById(id);
    }
}
