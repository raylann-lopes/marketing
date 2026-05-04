package com.north.producoes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

@Configuration
public class MetaGraphConfig {

    @Bean
    public RestClient metaGraphRestClient(
            RestClient.Builder builder,
            @Value("${meta.graph.base-url:https://graph.facebook.com}") String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
