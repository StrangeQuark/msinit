package com.example.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    @Value("${rate-limit.auth.replenish-rate}")
    private int authReplenishRate;
    @Value("${rate-limit.auth.burst-capacity}")
    private int authBurstCapacity;

    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> {
            if(exchange.getRequest().getRemoteAddress() == null)
                return Mono.just("unknown");

            return Mono.just(exchange.getRequest().getRemoteAddress().getHostString());
        };
    }

    @Bean
    @Primary
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(authReplenishRate, authBurstCapacity, 1);
    }
}
