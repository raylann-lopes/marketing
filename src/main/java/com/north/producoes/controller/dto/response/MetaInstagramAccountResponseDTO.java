package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.integration.meta.dto.MetaPageResponseDTO;

public record MetaInstagramAccountResponseDTO(
        String pageId,
        String pageName,
        String igUserId,
        String igUsername,
        String igName,
        boolean alreadyLinked,
        Long linkedClientId,
        String linkedClientName
) {
    public static MetaInstagramAccountResponseDTO from(MetaPageResponseDTO page) {
        return new MetaInstagramAccountResponseDTO(
                page.id(),
                page.name(),
                page.instagramBusinessAccount().id(),
                page.instagramBusinessAccount().username(),
                page.instagramBusinessAccount().name(),
                false,
                null,
                null
        );
    }

    public static MetaInstagramAccountResponseDTO from(MetaPageResponseDTO page, AccountConfigEntity config) {
        return new MetaInstagramAccountResponseDTO(
                page.id(),
                page.name(),
                page.instagramBusinessAccount().id(),
                page.instagramBusinessAccount().username(),
                page.instagramBusinessAccount().name(),
                true,
                config.getClient().getId(),
                config.getClient().getName()
        );
    }
}
