package a.slelin.work.voting.server.service;

import a.slelin.work.voting.common.dto.OptionResponse;
import a.slelin.work.voting.common.dto.PollCreateRequest;
import a.slelin.work.voting.common.dto.PollResponse;
import a.slelin.work.voting.common.dto.PollSummaryResponse;
import a.slelin.work.voting.common.dto.VoteRequest;
import a.slelin.work.voting.server.exception.InvalidPollException;
import a.slelin.work.voting.server.exception.PollNotFoundException;
import a.slelin.work.voting.server.repository.PollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PollServiceTest {

    private PollService service;

    @BeforeEach
    void setUp() {
        service = new PollService(new PollRepository(), new PollMapper());
    }

    @Test
    void createTrimsInputAndDropsBlankDescription() {
        PollResponse poll = service.create(new PollCreateRequest("  Question? ", "   ", List.of(" Yes ", "No")));

        assertThat(poll.question()).isEqualTo("Question?");
        assertThat(poll.description()).isNull();
        assertThat(poll.options()).extracting(OptionResponse::text).containsExactly("Yes", "No");
        assertThat(poll.totalVotes()).isZero();
        assertThat(poll.votedOptionId()).isNull();
    }

    @Test
    void createRejectsDuplicateOptions() {
        assertThatThrownBy(() -> service.create(new PollCreateRequest("Q", null, List.of("Java", " java"))))
                .isInstanceOf(InvalidPollException.class);
    }

    @Test
    void voteReturnsPercentsAndVoterChoice() {
        UUID id = service.create(new PollCreateRequest("Q", null, List.of("A", "B", "C"))).id();
        service.vote(id, new VoteRequest("Anna", 1L));
        service.vote(id, new VoteRequest("Boris", 2L));
        PollResponse poll = service.vote(id, new VoteRequest("Vika", 2L));

        assertThat(poll.totalVotes()).isEqualTo(3);
        assertThat(poll.votedOptionId()).isEqualTo(2L);
        assertThat(poll.options()).extracting(OptionResponse::percent).containsExactly(33.3, 66.7, 0.0);
        assertThat(poll.leader()).map(OptionResponse::text).contains("B");
    }

    @Test
    void openPollsGoFirstAndNewestFirst() {
        UUID first = service.create(new PollCreateRequest("First", null, List.of("A", "B"))).id();
        UUID second = service.create(new PollCreateRequest("Second", null, List.of("A", "B"))).id();
        UUID third = service.create(new PollCreateRequest("Third", null, List.of("A", "B"))).id();
        service.close(third, null);

        assertThat(service.findAll()).extracting(PollSummaryResponse::id).containsExactly(second, first, third);
    }

    @Test
    void deleteUnknownPollFails() {
        assertThatThrownBy(() -> service.delete(UUID.randomUUID())).isInstanceOf(PollNotFoundException.class);
    }
}
