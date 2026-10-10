package a.slelin.work.voting.common.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PollSummaryResponse(UUID id,
                                  String question,
                                  PollStatus status,
                                  int optionsCount,
                                  long totalVotes,
                                  Instant createdAt) {
}
