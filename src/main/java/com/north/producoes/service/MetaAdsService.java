package com.north.producoes.service;

import com.north.producoes.controller.dto.response.MetaAdsAccountOptionResponseDTO;
import com.north.producoes.integration.meta.MetaGraphClient;
import com.north.producoes.integration.meta.dto.MetaAdsAccountDTO;
import com.north.producoes.integration.meta.dto.MetaAdsAccountResponseDTO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class MetaAdsService {

    private final MetaGraphClient metaGraphClient;

    public List<MetaAdsAccountOptionResponseDTO> findAdsAccounts() {
        return findAccounts()
                .stream()
                .filter(account -> account.accountId() != null && !account.accountId().isBlank())
                .map(MetaAdsAccountOptionResponseDTO::from)
                .toList();
    }

    private List<MetaAdsAccountDTO> findAccounts() {
        MetaAdsAccountResponseDTO response = metaGraphClient.getAdAccounts();
        if (response == null || response.data() == null) {
            return Collections.emptyList();
        }
        return response.data();
    }
}
