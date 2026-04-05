package com.north.producoes.controller.dto.response;

public record PresignedUploadResponseDTO(
        String uploadUrl,   // URL presigned para PUT direto no S3 (expira em 15 min)
        String s3Key        // Chave a ser salva no ApproveRequestDTO.artS3Key
) {}
