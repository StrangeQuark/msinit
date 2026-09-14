package com.example.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.WebFilter;

import java.util.List;

@Configuration
public class CorsPreflightConfig {
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter corsPreflightFilter() {
        return (exchange, chain) -> {
            if (!CorsUtils.isPreFlightRequest(exchange.getRequest()))
                return chain.filter(exchange);

            String origin = exchange.getRequest().getHeaders().getOrigin();
            if (origin == null || !allowedOrigins.contains(origin)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.setAccessControlAllowOrigin(origin);
            headers.setAccessControlAllowCredentials(true);
            headers.setAccessControlAllowMethods(List.of(exchange.getRequest().getHeaders().getAccessControlRequestMethod()));
            headers.setAccessControlAllowHeaders(exchange.getRequest().getHeaders().getAccessControlRequestHeaders());
            headers.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            headers.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
            headers.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

            return exchange.getResponse().setComplete();
        };
    }
}
