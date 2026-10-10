package a.slelin.work.voting.server.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PollNotFoundException extends VotingException {

    public PollNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Голосование %s не найдено".formatted(id));
    }
}
