package ru.mts.depositservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class AppConfig {
    /**
     * Создает и настраивает бин {@link RestTemplate} с использованием {@link HttpComponentsClientHttpRequestFactory}
     * <p>
     * Используется для выполнения HTTP-запросов к внешним сервисам
     *
     * @return Бин {@link RestTemplate}, настроенный для выполнения HTTP-запросов
     */
    @Bean
    RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }
}
