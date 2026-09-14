package com.example.gatewayservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class HostsConfig {
    @Value("${hosts}")
    private String hostsEnv;

    private Map<String, String> hosts;

    @PostConstruct
    public void parseHosts() {
        hosts = Arrays.stream(hostsEnv.split(","))
                .map(entry -> entry.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        e -> e[0].trim(),
                        e -> e[1].trim()
                ));
    }

    public Map<String, String> getHosts() {
        return hosts;
    }
}
