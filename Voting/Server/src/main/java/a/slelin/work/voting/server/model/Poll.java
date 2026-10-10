package a.slelin.work.voting.server.model;

import a.slelin.work.voting.common.dto.PollStatus;
import a.slelin.work.voting.server.exception.AlreadyVotedException;
import a.slelin.work.voting.server.exception.OptionNotFoundException;
import a.slelin.work.voting.server.exception.PollClosedException;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Голосование. Хранит варианты ответа и голоса участников (один участник — один голос).
 * Все изменяемые операции синхронизированы: голосовать могут одновременно несколько клиентов.
 */
@Getter
public class Poll {

    private final UUID id;
    private final String question;
    private final String description;
    private final List<PollOption> options;
    private final Instant createdAt;

    private volatile PollStatus status = PollStatus.OPEN;
    private volatile Instant closedAt;

    /**
     * Участник (в нижнем регистре) → id выбранного варианта.
     */
    private final Map<String, Long> votes = new HashMap<>();

    public Poll(String question, String description, List<String> options) {
        this.id = UUID.randomUUID();
        this.question = question;
        this.description = description;
        this.options = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            this.options.add(new PollOption(i + 1, options.get(i)));
        }
        this.createdAt = Instant.now();
    }

    public synchronized void vote(String voter, long optionId) {
        if (status == PollStatus.CLOSED) {
            throw new PollClosedException(id);
        }
        if (options.stream().noneMatch(o -> o.id() == optionId)) {
            throw new OptionNotFoundException(id, optionId);
        }

        String key = normalize(voter);
        if (votes.containsKey(key)) {
            throw new AlreadyVotedException(id, voter.trim());
        }
        votes.put(key, optionId);
    }

    public synchronized void close() {
        if (status == PollStatus.OPEN) {
            status = PollStatus.CLOSED;
            closedAt = Instant.now();
        }
    }

    /**
     * Снимок подсчёта голосов: id варианта → количество голосов (в порядке вариантов).
     */
    public synchronized Map<Long, Long> tally() {
        Map<Long, Long> result = new LinkedHashMap<>();
        options.forEach(o -> result.put(o.id(), 0L));
        votes.values().forEach(optionId -> result.merge(optionId, 1L, Long::sum));
        return result;
    }

    public synchronized long totalVotes() {
        return votes.size();
    }

    public synchronized Optional<Long> choiceOf(String voter) {
        return voter == null || voter.isBlank()
                ? Optional.empty()
                : Optional.ofNullable(votes.get(normalize(voter)));
    }

    private static String normalize(String voter) {
        return voter.trim().toLowerCase(Locale.ROOT);
    }
}
