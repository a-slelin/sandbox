package a.slelin.work.voting.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient votingRestClient(RestClient.Builder builder, ClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return builder
                .baseUrl(properties.serverUrl())
                .requestFactory(requestFactory)
                // Сообщения валидации от сервера приходят на русском
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ru")
                .build();
    }
}
