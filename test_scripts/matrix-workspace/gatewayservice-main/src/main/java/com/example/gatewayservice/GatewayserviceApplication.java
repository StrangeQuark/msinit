package com.example.gatewayservice;

import com.example.gatewayservice.config.HostsConfig;
import com.example.gatewayservice.config.RateLimitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayserviceApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(GatewayserviceApplication.class);

	@Autowired
    private HostsConfig hostsConfig;
	@Autowired
	private RateLimitConfig rateLimitConfig;
	@Value("${service.jenkins.url}")
	private String jenkinsServiceUrl;
	@Value("${jenkinsservice.integration}")
	private boolean jenkinsserviceIntegration;

	@Value("${service.auth.url}")
	private String authServiceUrl;
	@Value("${authservice.integration}")
	private boolean authserviceIntegration;

	@Value("${service.email.url}")
	private String emailServiceUrl;
	@Value("${emailservice.integration}")
	private boolean emailserviceIntegration;

	@Value("${service.file.url}")
	private String fileServiceUrl;
	@Value("${fileservice.integration}")
	private boolean fileserviceIntegration;

	@Value("${service.vault.url}")
	private String vaultServiceUrl;
	@Value("${vaultservice.integration}")
	private boolean vaultserviceIntegration;

	@Value("${service.telemetry.url}")
	private String telemetryServiceUrl;
	@Value("${telemetryservice.integration}")
	private boolean telemetryserviceIntegration;

	@Value("${service.react.url}")
	private String reactServiceUrl;
	@Value("${reactservice.integration}")
	private boolean reactserviceIntegration;

	public static void main(String[] args) {
		LOGGER.info("Starting Gateway Service Application...");
		SpringApplication.run(GatewayserviceApplication.class, args);
		LOGGER.info("Gateway Service Application started.");
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		RouteLocatorBuilder.Builder routes = builder.routes();

		if(jenkinsserviceIntegration)
			routes.route("jenkins_route", r -> r.host(hostsConfig.getHosts().get("jenkins"))
						.and()
						.path("/**")
						.filters(f -> f
								.addResponseHeader("X-Powered-By", "Gateway Service"))
						.uri(jenkinsServiceUrl)
			);

		if(authserviceIntegration) {
			routes.route(r -> r.path("/api/auth/health")
						.filters(f -> f
								.addResponseHeader("X-Powered-By", "Gateway Service"))
						.uri(authServiceUrl)
			);
			routes.route(r -> r.path("/api/auth/**")
						.filters(f -> f
								.requestRateLimiter(config -> config
										.setRateLimiter(rateLimitConfig.authRateLimiter())
										.setKeyResolver(rateLimitConfig.clientIpKeyResolver()))
								.addResponseHeader("X-Powered-By", "Gateway Service"))
						.uri(authServiceUrl)
			);
		}

		if(emailserviceIntegration)
			routes.route(r -> r.path("/api/email/**")
						.filters(f -> f
								.addResponseHeader("X-Powered-By", "Gateway Service"))
						.uri(emailServiceUrl)
			);

		if(fileserviceIntegration)
			routes.route(r -> r.path("/api/file/**")
						.filters(f -> f
								.addResponseHeader("X-Powered-By", "Gateway Service"))
						.uri(fileServiceUrl)
			);

		if(vaultserviceIntegration)
			routes.route(r -> r.path("/api/vault/**")
						.filters(f -> f
								.addResponseHeader("X-Powered-By", "Gateway Service"))
						.uri(vaultServiceUrl)
			);

		if(telemetryserviceIntegration)
			routes.route(r -> r.path("/api/telemetry/**")
                        .filters(f -> f
                                .addResponseHeader("X-Powered-By", "Gateway Service"))
                        .uri(telemetryServiceUrl)
			);

		if(reactserviceIntegration)
			routes.route(r -> r.path("/**")
						.and()
						.not(p -> p.path("/api/**"))
						.filters(f -> f
								.addResponseHeader("X-Powered-By", "Gateway Service"))
						.uri(reactServiceUrl)
			);

		return routes.build();
	}
}
