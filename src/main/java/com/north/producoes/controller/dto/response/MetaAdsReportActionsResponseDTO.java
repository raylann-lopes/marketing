package com.north.producoes.controller.dto.response;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record MetaAdsReportActionsResponseDTO(
        Long linkClicks,
        Long postEngagement,
        Long postReactions,
        Long videoViews,
        Long conversationsStarted,
        Long messagingConnections
) {
    public static MetaAdsReportActionsResponseDTO from(List<MetaAdsActionResponseDTO> actions){
        return new MetaAdsReportActionsResponseDTO(
                valueOf(actions, "link_click"),
                valueOf(actions, "post_engagement"),
                valueOf(actions, "post_reaction"),
                valueOf(actions, "video_view"),
                valueOf(actions, "onsite_conversion.messaging_conversation_started_7d"),
                valueOf(actions, "onsite_conversion.total_messaging_connection")
        );
    }

    private static Long valueOf(List<MetaAdsActionResponseDTO> actions, String type) {
        return Objects.requireNonNullElse(actions, Collections.<MetaAdsActionResponseDTO>emptyList())
                .stream()
                .filter(action -> type.equals(action.type()))
                .map(MetaAdsActionResponseDTO::value)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
    }
}
