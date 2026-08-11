package com.north.producoes.service;

import com.north.producoes.controller.dto.request.MetaAdsAccountInsightRequestDTO;
import com.north.producoes.controller.dto.response.AdsReportSnapshotResponseDTO;
import com.north.producoes.controller.dto.response.MetaAdsAccountInsightResponseDTO;
import com.north.producoes.controller.dto.response.MetaAdsAccountOptionResponseDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.integration.meta.MetaGraphClient;
import com.north.producoes.integration.meta.dto.MetaAdsAccountDTO;
import com.north.producoes.integration.meta.dto.MetaAdsAccountResponseDTO;
import com.north.producoes.integration.meta.dto.MetaAdsInsightsResponseDTO;
import com.north.producoes.repository.AccountConfigRepository;
import com.north.producoes.repository.ClientRepository;

import org.springframework.stereotype.Service;


import java.util.Collections;
import java.util.List;

@Service
public class MetaAdsService {

    private final MetaGraphClient metaGraphClient;
    private final AccountConfigRepository accountConfigRepository;
    private final ClientRepository clientRepository;

    public MetaAdsService(MetaGraphClient metaGraphClient,
                          AccountConfigRepository accountConfigRepository,
                          ClientRepository clientRepository) {
        this.metaGraphClient = metaGraphClient;
        this.accountConfigRepository = accountConfigRepository;
        this.clientRepository = clientRepository;
    }

    public List<MetaAdsAccountOptionResponseDTO> findAdsAccounts() {
        return adsAccounts()
                .stream()
                .filter(account -> account.accountId() != null && !account.accountId().isBlank())
                .map(MetaAdsAccountOptionResponseDTO::from)
                .toList();
    }

    public MetaAdsAccountInsightResponseDTO metaAdsInsights(MetaAdsAccountInsightRequestDTO request) {
        validateInsightsRequest(request);

        AccountConfigEntity config = accountConfigRepository.findByClientId(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Configuraçao nao encontrada para o cliente ID" + request.clientId()));

        String adsAccountId = config.getMetaAdAccountId();

        if (adsAccountId == null || adsAccountId.isBlank()) {
            throw new IllegalArgumentException("Meta Ads nao vinculado para este cliente");
        }

        MetaAdsInsightsResponseDTO response = metaGraphClient.getAdAccountInsights(adsAccountId, request.dateStart(), request.dateStop());
        if (response == null || response.data() == null) {
            throw new IllegalArgumentException("A meta nao retornou insights para o periodo indicado");
        }

        return response.data().stream()
                .findFirst()
                .map(MetaAdsAccountInsightResponseDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("A Meta não retornou dados para o período informado."));
    }

    public AdsReportSnapshotResponseDTO metaAdsReportSnapshot(MetaAdsAccountInsightRequestDTO request) {
        validateInsightsRequest(request);

        ClientEntity client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: id " + request.clientId()));

        MetaAdsAccountInsightResponseDTO insight = metaAdsInsights(request);

        return AdsReportSnapshotResponseDTO.from(client.getId(), client.getName(), insight);
    }

    private void validateInsightsRequest(MetaAdsAccountInsightRequestDTO request) {
        if (request == null || request.clientId() == null) {
            throw new IllegalArgumentException("Cliente é obrigatório");
        }
        if (request.dateStart() == null || request.dateStop() == null) {
            throw new IllegalArgumentException("Data inicial e data final são obrigatórias");
        }
        if (request.dateStart().isAfter(request.dateStop())) {
            throw new IllegalArgumentException("Data inicial deve ser anterior ou igual à data final");
        }
    }

    private List<MetaAdsAccountDTO> adsAccounts() {
        MetaAdsAccountResponseDTO response = metaGraphClient.getAdAccounts();
        if (response == null || response.data() == null) {
            return Collections.emptyList();
        }
        return response.data();
    }
}
