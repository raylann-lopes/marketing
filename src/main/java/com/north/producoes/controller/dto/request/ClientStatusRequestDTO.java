package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.ClientStatusEnum;
import jakarta.validation.constraints.NotNull;

public record ClientStatusRequestDTO(
    @NotNull(message = "Status é obrigatório")
    ClientStatusEnum status
) {}
