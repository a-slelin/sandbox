package a.slelin.work.voting.server.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PollClosedException extends VotingException {

    public PollClosedException(UUID id) {
        super(HttpStatus.CONFLICT, "Голосование %s уже завершено".formatted(id));
    }
}
