package a.slelin.work.voting.server.repository;

import a.slelin.work.voting.server.model.Poll;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище голосований в памяти: данные живут, пока запущен сервер.
 */
@Repository
public class PollRepository {

    private final Map<UUID, Poll> polls = new ConcurrentHashMap<>();

    public Poll save(Poll poll) {
        polls.put(poll.getId(), poll);
        return poll;
    }

    public Optional<Poll> findById(UUID id) {
        return Optional.ofNullable(polls.get(id));
    }

    public Collection<Poll> findAll() {
        return polls.values();
    }

    public boolean deleteById(UUID id) {
        return polls.remove(id) != null;
    }
}
