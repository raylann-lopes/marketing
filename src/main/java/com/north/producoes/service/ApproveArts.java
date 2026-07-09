package com.north.producoes.service;

import com.north.producoes.entity.ApproveCarouselArtEntity;
import com.north.producoes.entity.ApproveEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Ponto único de substituição das artes de uma aprovação — usado tanto pelo
 * fluxo de aprovação (ApprovedService) quanto pelo de upload (MediaService),
 * para que validação e espelhamento da capa nunca divirjam entre os dois.
 */
final class ApproveArts {

    private ApproveArts() {
    }

    record ArtRef(String s3Key, String artName) {
    }

    /**
     * Substitui as artes da aprovação. Com lista: recria em ordem e espelha a
     * primeira nos campos de capa (artS3Key/artName). Sem lista: fluxo legado
     * de imagem única via legacyS3Key/legacyArtName.
     */
    static void replace(S3Service s3Service, ApproveEntity approve, List<ArtRef> arts,
                        String legacyS3Key, String legacyArtName) {
        if (approve.getCarouselArts() != null) {
            approve.getCarouselArts().clear();
        } else {
            approve.setCarouselArts(new ArrayList<>());
        }

        if (arts != null && !arts.isEmpty()) {
            int order = 0;
            for (ArtRef item : arts) {
                String s3Key = normalizePublicS3Key(s3Service, item.s3Key());
                if (arts.size() > 1 && s3Service.isVideoKey(s3Key)) {
                    throw new IllegalArgumentException(
                            "Carrossel do Instagram suporta apenas imagens. Remova os vídeos: " + item.artName());
                }
                ApproveCarouselArtEntity artEntity = new ApproveCarouselArtEntity();
                artEntity.setApprove(approve);
                artEntity.setS3Key(s3Key);
                artEntity.setArtName(item.artName());
                artEntity.setSortOrder(order++);
                approve.getCarouselArts().add(artEntity);
            }
            approve.setArtS3Key(approve.getCarouselArts().get(0).getS3Key());
            approve.setArtName(approve.getCarouselArts().get(0).getArtName());
        } else {
            approve.setArtS3Key(normalizePublicS3Key(s3Service, legacyS3Key));
            approve.setArtName(legacyArtName);
        }
    }

    private static String normalizePublicS3Key(S3Service s3Service, String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            throw new IllegalArgumentException("s3Key é obrigatória");
        }
        String normalized = s3Key.trim();
        if (!s3Service.isPublicKey(normalized)) {
            throw new IllegalArgumentException(
                    "s3Key deve usar o prefixo público " + s3Service.getPublicPrefix() + "/");
        }
        return normalized;
    }
}
