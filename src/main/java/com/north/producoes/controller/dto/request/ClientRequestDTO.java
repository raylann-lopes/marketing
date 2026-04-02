package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequestDTO(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String number,
        @NotBlank String driveLink,
        String voiceTone,
        String niche
) {}
