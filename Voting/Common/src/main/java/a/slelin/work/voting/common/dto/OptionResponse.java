package a.slelin.work.voting.common.dto;

import lombok.Builder;

@Builder
public record OptionResponse(long id,
                             String text,
                             long votes,
                             double percent) {
}
