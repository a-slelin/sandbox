package a.slelin.work.voting.server.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OptionNotFoundException extends VotingException {

    public OptionNotFoundException(UUID pollId, long optionId) {
        super(HttpStatus.NOT_FOUND, "В голосовании %s нет варианта %d".formatted(pollId, optionId));
    }
}
