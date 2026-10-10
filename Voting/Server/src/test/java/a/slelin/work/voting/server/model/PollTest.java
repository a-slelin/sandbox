package a.slelin.work.voting.server.model;

import a.slelin.work.voting.common.dto.PollStatus;
import a.slelin.work.voting.server.exception.AlreadyVotedException;
import a.slelin.work.voting.server.exception.OptionNotFoundException;
import a.slelin.work.voting.server.exception.PollClosedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PollTest {

    private Poll poll;

    @BeforeEach
    void setUp() {
        poll = new Poll("Question", null, List.of("A", "B", "C"));
    }

    @Test
    void optionsAreNumberedFromOne() {
        assertThat(poll.getOptions()).extracting(PollOption::id).containsExactly(1L, 2L, 3L);
        assertThat(poll.getStatus()).isEqualTo(PollStatus.OPEN);
    }

    @Test
    void tallyCountsVotesPerOption() {
        poll.vote("Anna", 1);
        poll.vote("Boris", 1);
        poll.vote("Vika", 3);

        assertThat(poll.tally()).containsExactly(Map.entry(1L, 2L), Map.entry(2L, 0L), Map.entry(3L, 1L));
        assertThat(poll.totalVotes()).isEqualTo(3);
    }

    @Test
    void voterCanVoteOnlyOnceIgnoringCaseAndSpaces() {
        poll.vote("Anna", 1);

        assertThatThrownBy(() -> poll.vote("  anna ", 2)).isInstanceOf(AlreadyVotedException.class);
        assertThat(poll.choiceOf("ANNA")).contains(1L);
        assertThat(poll.choiceOf("Boris")).isEmpty();
        assertThat(poll.choiceOf(null)).isEmpty();
    }

    @Test
    void unknownOptionIsRejected() {
        assertThatThrownBy(() -> poll.vote("Anna", 42)).isInstanceOf(OptionNotFoundException.class);
        assertThat(poll.totalVotes()).isZero();
    }

    @Test
    void closedPollRejectsVotes() {
        poll.close();

        assertThat(poll.getStatus()).isEqualTo(PollStatus.CLOSED);
        assertThat(poll.getClosedAt()).isNotNull();
        assertThatThrownBy(() -> poll.vote("Anna", 1)).isInstanceOf(PollClosedException.class);
    }
}
