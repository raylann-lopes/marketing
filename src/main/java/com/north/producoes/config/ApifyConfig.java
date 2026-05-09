package com.north.producoes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ApifyConfig {

    @Bean
    public RestClient apifyRestClient(
            RestClient.Builder builder,
            @Value("${apify.api.base-url:https://api.apify.com}") String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
