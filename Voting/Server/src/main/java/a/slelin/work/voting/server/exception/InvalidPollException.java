package a.slelin.work.voting.server.exception;

import org.springframework.http.HttpStatus;

public class InvalidPollException extends VotingException {

    public InvalidPollException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
