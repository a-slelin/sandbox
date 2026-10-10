package a.slelin.work.voting.server.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AlreadyVotedException extends VotingException {

    public AlreadyVotedException(UUID pollId, String voter) {
        super(HttpStatus.CONFLICT, "Участник «%s» уже проголосовал в голосовании %s".formatted(voter, pollId));
    }
}
