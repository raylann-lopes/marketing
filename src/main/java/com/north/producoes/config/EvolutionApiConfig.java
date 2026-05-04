package com.north.producoes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class EvolutionApiConfig {

    @Bean
    public RestClient evolutionApiRestClient(
            RestClient.Builder builder,
            @Value("${evolution.api.base-url:http://localhost:8080}") String baseUrl
        ){
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
