package a.slelin.work.voting.server.service;

import a.slelin.work.voting.common.dto.OptionResponse;
import a.slelin.work.voting.common.dto.PollResponse;
import a.slelin.work.voting.common.dto.PollSummaryResponse;
import a.slelin.work.voting.server.entity.Poll;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PollMapper {

    /**
     * @param tally         id варианта → количество голосов (варианты без голосов могут отсутствовать)
     * @param votedOptionId за какой вариант проголосовал запросивший участник, если проголосовал
     */
    public PollResponse toResponse(Poll poll, Map<Long, Long> tally, Long votedOptionId) {
        long total = tally.values().stream().mapToLong(Long::longValue).sum();

        return PollResponse.builder()
                .id(poll.getId())
                .question(poll.getQuestion())
                .description(poll.getDescription())
                .status(poll.getStatus())
                .options(poll.getOptions().stream()
                        .map(o -> {
                            long votes = tally.getOrDefault(o.getId(), 0L);
                            return OptionResponse.builder()
                                    .id(o.getId())
                                    .text(o.getText())
                                    .votes(votes)
                                    .percent(total == 0 ? 0 : Math.round(votes * 1000.0 / total) / 10.0)
                                    .build();
                        })
                        .toList())
                .totalVotes(total)
                .createdAt(poll.getCreatedAt())
                .closedAt(poll.getClosedAt())
                .votedOptionId(votedOptionId)
                .build();
    }

    public PollSummaryResponse toSummary(Poll poll) {
        return PollSummaryResponse.builder()
                .id(poll.getId())
                .question(poll.getQuestion())
                .status(poll.getStatus())
                .optionsCount(poll.getOptionsCount())
                .totalVotes(poll.getTotalVotes())
                .createdAt(poll.getCreatedAt())
                .build();
    }
}
