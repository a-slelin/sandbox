package a.slelin.work.voting.server.service;

import a.slelin.work.voting.common.dto.OptionResponse;
import a.slelin.work.voting.common.dto.PollResponse;
import a.slelin.work.voting.common.dto.PollSummaryResponse;
import a.slelin.work.voting.server.model.Poll;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PollMapper {

    public PollResponse toResponse(Poll poll, String voter) {
        Map<Long, Long> tally = poll.tally();
        long total = tally.values().stream().mapToLong(Long::longValue).sum();

        return PollResponse.builder()
                .id(poll.getId())
                .question(poll.getQuestion())
                .description(poll.getDescription())
                .status(poll.getStatus())
                .options(poll.getOptions().stream()
                        .map(o -> {
                            long votes = tally.get(o.id());
                            return OptionResponse.builder()
                                    .id(o.id())
                                    .text(o.text())
                                    .votes(votes)
                                    .percent(total == 0 ? 0 : Math.round(votes * 1000.0 / total) / 10.0)
                                    .build();
                        })
                        .toList())
                .totalVotes(total)
                .createdAt(poll.getCreatedAt())
                .closedAt(poll.getClosedAt())
                .votedOptionId(poll.choiceOf(voter).orElse(null))
                .build();
    }

    public PollSummaryResponse toSummary(Poll poll) {
        return PollSummaryResponse.builder()
                .id(poll.getId())
                .question(poll.getQuestion())
                .status(poll.getStatus())
                .optionsCount(poll.getOptions().size())
                .totalVotes(poll.totalVotes())
                .createdAt(poll.getCreatedAt())
                .build();
    }
}
