package a.slelin.work.voting.client.api;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ошибка обращения к серверу: либо ответ с кодом ошибки, либо сервер недоступен ({@code status == 0}).
 */
@Getter
public class ApiException extends RuntimeException {

    private final int status;
    private final Map<String, String> details;

    public ApiException(int status, String message, Map<String, String> details) {
        super(message);
        this.status = status;
        this.details = details == null ? Map.of() : details;
    }

    public boolean isConnectionError() {
        return status == 0;
    }

    public boolean isNotFound() {
        return status == 404;
    }

    /**
     * Сообщение для пользователя вместе с подробностями валидации.
     */
    public String userMessage() {
        if (details.isEmpty()) {
            return getMessage();
        }
        return getMessage() + ": " + details.entrySet().stream()
                .map(e -> e.getKey() + " — " + e.getValue())
                .collect(Collectors.joining("; "));
    }
}
