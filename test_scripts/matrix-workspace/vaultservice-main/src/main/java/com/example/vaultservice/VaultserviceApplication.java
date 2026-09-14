package com.example.vaultservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class VaultserviceApplication {

    @Value("${service.http.connect.timeout}")
    private int serviceHttpConnectTimeout;

    @Value("${service.http.read.timeout}")
    private int serviceHttpReadTimeout;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(serviceHttpConnectTimeout);
        requestFactory.setReadTimeout(serviceHttpReadTimeout);

        return new RestTemplate(requestFactory);
    }

	public static void main(String[] args) {
		SpringApplication.run(VaultserviceApplication.class, args);
	}

}
