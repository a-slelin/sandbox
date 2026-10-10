package a.slelin.work.voting.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record VoteRequest(@NotBlank @Size(max = 50) String voter,
                          @NotNull Long optionId) {
}
