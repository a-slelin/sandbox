package a.slelin.work.voting.server.service;

import a.slelin.work.voting.common.dto.PollCreateRequest;
import a.slelin.work.voting.common.dto.PollResponse;
import a.slelin.work.voting.common.dto.PollSummaryResponse;
import a.slelin.work.voting.common.dto.VoteRequest;
import a.slelin.work.voting.server.entity.Poll;
import a.slelin.work.voting.server.entity.PollOption;
import a.slelin.work.voting.server.entity.Vote;
import a.slelin.work.voting.server.exception.AlreadyVotedException;
import a.slelin.work.voting.server.exception.InvalidPollException;
import a.slelin.work.voting.server.exception.OptionNotFoundException;
import a.slelin.work.voting.server.exception.PollClosedException;
import a.slelin.work.voting.server.exception.PollNotFoundException;
import a.slelin.work.voting.server.repository.PollRepository;
import a.slelin.work.voting.server.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollService {

    /**
     * Сначала открытые голосования ('OPEN' > 'CLOSED' по алфавиту), внутри — от новых к старым.
     */
    private static final Sort LIST_ORDER = Sort.by(Sort.Order.desc("status"), Sort.Order.desc("createdAt"));

    private final PollRepository pollRepository;
    private final VoteRepository voteRepository;
    private final PollMapper mapper;

    @Transactional(readOnly = true)
    public List<PollSummaryResponse> findAll() {
        return pollRepository.findAll(LIST_ORDER).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PollResponse get(UUID id, String voter) {
        return toResponse(find(id), voter);
    }

    @Transactional
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

        Poll poll = pollRepository.save(new Poll(request.question().trim(), description, options));
        log.info("Created poll {}: {}", poll.getId(), poll.getQuestion());
        return toResponse(poll, null);
    }

    @Transactional
    public PollResponse vote(UUID id, VoteRequest request) {
        Poll poll = pollRepository.findByIdForUpdate(id).orElseThrow(() -> new PollNotFoundException(id));
        if (!poll.isOpen()) {
            throw new PollClosedException(id);
        }

        PollOption option = poll.getOptions().stream()
                .filter(o -> o.getId().equals(request.optionId()))
                .findFirst()
                .orElseThrow(() -> new OptionNotFoundException(id, request.optionId()));

        String voter = request.voter().trim();
        if (voteRepository.existsByPollIdAndVoter(id, Vote.normalize(voter))) {
            throw new AlreadyVotedException(id, voter);
        }
        try {
            voteRepository.saveAndFlush(new Vote(poll, option, voter));
        } catch (DataIntegrityViolationException e) {
            // Страховка на уровне базы: уникальный индекс (poll_id, voter)
            throw new AlreadyVotedException(id, voter);
        }

        log.info("Vote in poll {}: {} -> option {}", id, voter, option.getId());
        return toResponse(poll, voter);
    }

    @Transactional
    public PollResponse close(UUID id, String voter) {
        Poll poll = pollRepository.findByIdForUpdate(id).orElseThrow(() -> new PollNotFoundException(id));
        poll.close();
        log.info("Closed poll {}", id);
        return toResponse(poll, voter);
    }

    @Transactional
    public void delete(UUID id) {
        if (!pollRepository.existsById(id)) {
            throw new PollNotFoundException(id);
        }
        voteRepository.deleteByPollId(id);
        pollRepository.deleteById(id);
        log.info("Deleted poll {}", id);
    }

    private Poll find(UUID id) {
        return pollRepository.findById(id).orElseThrow(() -> new PollNotFoundException(id));
    }

    private PollResponse toResponse(Poll poll, String voter) {
        Map<Long, Long> tally = voteRepository.countByOption(poll.getId()).stream()
                .collect(Collectors.toMap(VoteRepository.OptionVotes::getOptionId, VoteRepository.OptionVotes::getVotes));
        Long votedOptionId = voter == null || voter.isBlank() ? null
                : voteRepository.findOptionId(poll.getId(), Vote.normalize(voter)).orElse(null);
        return mapper.toResponse(poll, tally, votedOptionId);
    }
}
