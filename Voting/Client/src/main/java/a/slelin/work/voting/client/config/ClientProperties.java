package a.slelin.work.voting.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Настройки клиента из {@code application.yaml} (префикс {@code voting.client}).
 *
 * @param serverUrl       адрес сервера голосований
 * @param voter           имя участника; если пустое — спрашивается при запуске
 * @param refreshInterval как часто подтягивать свежие результаты с сервера
 * @param darkTheme       запускаться в тёмной теме
 */
@ConfigurationProperties("voting.client")
public record ClientProperties(@DefaultValue("http://localhost:8080") String serverUrl,
                               @DefaultValue("") String voter,
                               @DefaultValue("3s") Duration refreshInterval,
                               @DefaultValue("false") boolean darkTheme) {
}
