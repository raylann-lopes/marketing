package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MetaAdsAccountLinkRequestDTO(
        @NotBlank(message = "Conta de anuncios e obrigatoria")
        @Pattern(regexp = "^(act_)?\\d+$", message = "Conta de anuncios deve conter apenas digitos ou o prefixo act_")
        String metaAdAccountId
) {}
