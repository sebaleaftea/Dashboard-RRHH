package com.microservice_employee.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.io.IOException;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

@Configuration
@Profile("talana")
public class RestTemplateConfig {

    @Value("${talana.api.token}")
    private String apiToken;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(20))
            .setReadTimeout(Duration.ofSeconds(60))
            .additionalInterceptors(authInterceptor(), new RetryInterceptor())
            .build();
    }

    private ClientHttpRequestInterceptor authInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().add("Authorization", "Token " + apiToken);
            request.getHeaders().add("Content-Type", "application/json");
            return execution.execute(request, body);
        };
    }

    private static class RetryInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            int maxRetries = 3;
            int retryCount = 0;
            IOException lastException = null;
            while (retryCount < maxRetries) {
                try {
                    ClientHttpResponse response = execution.execute(request, body);
                    if (response.getStatusCode().is5xxServerError()) {
                        retryCount++;
                        try { Thread.sleep(1000L * retryCount); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    } else {
                        return response;
                    }
                } catch (IOException e) {
                    lastException = e;
                    retryCount++;
                    try { Thread.sleep(1000L * retryCount); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
            throw lastException != null ? lastException : new IOException("Error después de " + maxRetries + " intentos");
        }
    }
}
