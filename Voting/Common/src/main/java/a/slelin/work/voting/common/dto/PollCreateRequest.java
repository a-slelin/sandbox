package a.slelin.work.voting.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record PollCreateRequest(@NotBlank @Size(max = 200) String question,

                                @Size(max = 500) String description,

                                @NotNull @Size(min = 2, max = 10)
                                List<@NotBlank @Size(max = 100) String> options) {
}
