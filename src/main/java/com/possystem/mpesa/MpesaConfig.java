package com.possystem.mpesa;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
public class MpesaConfig {

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.short-code}")
    private String shortCode;

    @Value("${mpesa.pass-key}")
    private String passKey;

    @Value("${mpesa.callback-url}")
    private String callbackUrl;

    @Value("${mpesa.base-url}")
    private String baseUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
