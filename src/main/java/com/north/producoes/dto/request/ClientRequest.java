package com.north.producoes.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String number,
        @NotBlank String driveLink,
        String voiceTone,
        String niche
) {}
