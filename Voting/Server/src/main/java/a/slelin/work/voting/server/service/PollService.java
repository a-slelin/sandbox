package a.slelin.work.voting.server.service;

import a.slelin.work.voting.common.dto.PollCreateRequest;
import a.slelin.work.voting.common.dto.PollResponse;
import a.slelin.work.voting.common.dto.PollStatus;
import a.slelin.work.voting.common.dto.PollSummaryResponse;
import a.slelin.work.voting.common.dto.VoteRequest;
import a.slelin.work.voting.server.exception.InvalidPollException;
import a.slelin.work.voting.server.exception.PollNotFoundException;
import a.slelin.work.voting.server.model.Poll;
import a.slelin.work.voting.server.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollService {

    /**
     * Сначала открытые голосования, внутри — от новых к старым.
     */
    private static final Comparator<Poll> LIST_ORDER = Comparator
            .comparing((Poll p) -> p.getStatus() == PollStatus.CLOSED)
            .thenComparing(Poll::getCreatedAt, Comparator.reverseOrder());

    private final PollRepository repository;
    private final PollMapper mapper;

    public List<PollSummaryResponse> findAll() {
        return repository.findAll().stream()
                .sorted(LIST_ORDER)
                .map(mapper::toSummary)
                .toList();
    }

    public PollResponse get(UUID id, String voter) {
        return mapper.toResponse(find(id), voter);
    }

    public PollResponse create(PollCreateRequest request) {
        List<String> options = request.options().stream().map(String::trim).toList();

        Set<String> unique = new HashSet<>();
        for (String option : options) {
            if (!unique.add(option.toLowerCase(Locale.ROOT))) {
                throw new InvalidPollException("Вариант «%s» указан несколько раз".formatted(option));
            }
        }

        String description = request.description() == null || request.description().isBlank()
                ? null : request.description().trim();

        Poll poll = repository.save(new Poll(request.question().trim(), description, options));
        log.info("Created poll {}: {}", poll.getId(), poll.getQuestion());
        return mapper.toResponse(poll, null);
    }

    public PollResponse vote(UUID id, VoteRequest request) {
        Poll poll = find(id);
        poll.vote(request.voter(), request.optionId());
        log.info("Vote in poll {}: {} -> option {}", id, request.voter(), request.optionId());
        return mapper.toResponse(poll, request.voter());
    }

    public PollResponse close(UUID id, String voter) {
        Poll poll = find(id);
        poll.close();
        log.info("Closed poll {}", id);
        return mapper.toResponse(poll, voter);
    }

    public void delete(UUID id) {
        if (!repository.deleteById(id)) {
            throw new PollNotFoundException(id);
        }
        log.info("Deleted poll {}", id);
    }

    private Poll find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PollNotFoundException(id));
    }
}
