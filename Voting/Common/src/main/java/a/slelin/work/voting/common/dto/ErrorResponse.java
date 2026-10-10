package a.slelin.work.voting.common.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record ErrorResponse(int status,
                            String error,
                            String message,
                            String path,
                            Map<String, String> details,
                            Instant timestamp) {
}
