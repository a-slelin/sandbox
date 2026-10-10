package a.slelin.work.voting.common.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Полное состояние голосования.
 * {@code votedOptionId} заполняется, если в запросе передан участник, который уже проголосовал.
 */
@Builder
public record PollResponse(UUID id,
                           String question,
                           String description,
                           PollStatus status,
                           List<OptionResponse> options,
                           long totalVotes,
                           Instant createdAt,
                           Instant closedAt,
                           Long votedOptionId) {

    public boolean acceptsVotes() {
        return status == PollStatus.OPEN;
    }

    public boolean hasVoted() {
        return votedOptionId != null;
    }

    public Optional<OptionResponse> leader() {
        return totalVotes == 0 ? Optional.empty() : options.stream()
                .reduce((a, b) -> b.votes() > a.votes() ? b : a);
    }
}
