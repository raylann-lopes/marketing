package com.north.producoes.service;

import com.north.producoes.controller.dto.request.AccountConfigRequestDTO;
import com.north.producoes.controller.dto.request.MetaInstagramAccountLinkRequestDTO;
import com.north.producoes.controller.dto.response.MetaAccountsResponseDTO;
import com.north.producoes.controller.dto.response.MetaInstagramAccountResponseDTO;
import com.north.producoes.integration.meta.dto.MetaPageResponseDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.exception.MetaGraphIntegrationException;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.integration.meta.MetaGraphClient;
import com.north.producoes.repository.AccountConfigRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class MetaGraphService {

    private final MetaGraphClient metaGraphClient;
    private final AccountConfigRepository accountConfigRepository;
    private final AccountConfigService accountConfigService;

    public List<MetaInstagramAccountResponseDTO> findInstagramAccounts() {
        return findPages()
                .stream()
                .filter(page -> page.instagramBusinessAccount() != null)
                .map(this::toResponse)
                .toList();
    }

    public AccountConfigEntity linkInstagramAccount(MetaInstagramAccountLinkRequestDTO request, String adminEmail) {
        MetaPageResponseDTO selectedPage = findPages().stream()
                .filter(page -> request.pageId().equals(page.id()))
                .filter(page -> page.instagramBusinessAccount() != null)
                .filter(page -> request.igUserId().equals(page.instagramBusinessAccount().id()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conta Instagram nao encontrada na Meta para a pagina informada."));

        String pageAccessToken = selectedPage.accessToken();
        if (!StringUtils.hasText(pageAccessToken)) {
            throw new MetaGraphIntegrationException("A Meta nao retornou token de acesso para a pagina selecionada.");
        }

        AccountConfigRequestDTO accountConfigRequest = new AccountConfigRequestDTO(
                request.clientId(),
                request.igUserId(),
                pageAccessToken
        );

        return accountConfigService.configure(accountConfigRequest, adminEmail);
    }

    private MetaInstagramAccountResponseDTO toResponse(MetaPageResponseDTO page) {
        String igUserId = page.instagramBusinessAccount().id();

        return accountConfigRepository.findByIgUserId(igUserId)
                .map(config -> MetaInstagramAccountResponseDTO.from(page, config))
                .orElseGet(() -> MetaInstagramAccountResponseDTO.from(page));
    }

    private List<MetaPageResponseDTO> findPages() {
        MetaAccountsResponseDTO response = metaGraphClient.findPages();
        if (response == null || response.data() == null) {
            return Collections.emptyList();
        }
        return response.data();
    }
}
