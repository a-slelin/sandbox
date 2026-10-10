package a.slelin.work.voting.server.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Базовое исключение бизнес-логики: содержит HTTP-статус, с которым ошибка уходит клиенту.
 */
@Getter
public abstract class VotingException extends RuntimeException {

    private final HttpStatus status;

    protected VotingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
