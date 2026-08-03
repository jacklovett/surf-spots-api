package com.lovettj.surfspotsapi.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "app.environmental-alerts.enabled", havingValue = "true")
public class EnvironmentalAlertHttpConfig {

    @Bean
    @ConditionalOnMissingBean(name = "environmentalAlertRestClient")
    public RestClient environmentalAlertRestClient(EnvironmentalAlertsProperties properties) {
        Duration connectTimeout = properties.getHttp().getConnectTimeout();
        Duration readTimeout = properties.getHttp().getReadTimeout();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Math.min(connectTimeout.toMillis(), Integer.MAX_VALUE));
        requestFactory.setReadTimeout((int) Math.min(readTimeout.toMillis(), Integer.MAX_VALUE));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
