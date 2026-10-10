package a.slelin.work.voting.client.api;

import a.slelin.work.voting.client.config.ClientProperties;
import a.slelin.work.voting.common.dto.ErrorResponse;
import a.slelin.work.voting.common.dto.PollCreateRequest;
import a.slelin.work.voting.common.dto.PollResponse;
import a.slelin.work.voting.common.dto.PollSummaryResponse;
import a.slelin.work.voting.common.dto.VoteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Обёртка над REST API сервера. Все методы блокирующие — вызывать их нужно не из потока Swing.
 */
@Component
@RequiredArgsConstructor
public class VotingApi {

    private static final String POLLS = "/api/polls";
    private static final String POLL = POLLS + "/{id}";

    private final RestClient rest;
    private final ClientProperties properties;

    public List<PollSummaryResponse> findAll() {
        return call(() -> rest.get()
                .uri(POLLS)
                .retrieve()
                .body(new ParameterizedTypeReference<List<PollSummaryResponse>>() {
                }));
    }

    public PollResponse get(UUID id, String voter) {
        return call(() -> rest.get()
                .uri(POLL + "?voter={voter}", id, voter)
                .retrieve()
                .body(PollResponse.class));
    }

    public PollResponse create(PollCreateRequest request) {
        return call(() -> rest.post()
                .uri(POLLS)
                .body(request)
                .retrieve()
                .body(PollResponse.class));
    }

    public PollResponse vote(UUID id, String voter, long optionId) {
        return call(() -> rest.post()
                .uri(POLL + "/votes", id)
                .body(new VoteRequest(voter, optionId))
                .retrieve()
                .body(PollResponse.class));
    }

    public PollResponse close(UUID id, String voter) {
        return call(() -> rest.post()
                .uri(POLL + "/close?voter={voter}", id, voter)
                .retrieve()
                .body(PollResponse.class));
    }

    public void delete(UUID id) {
        call(() -> rest.delete()
                .uri(POLL, id)
                .retrieve()
                .toBodilessEntity());
    }

    public String serverUrl() {
        return properties.serverUrl();
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            ErrorResponse error = readError(e);
            String message = error != null && error.message() != null
                    ? error.message()
                    : "Сервер ответил ошибкой " + e.getStatusCode().value();
            throw new ApiException(e.getStatusCode().value(), message, error == null ? null : error.details());
        } catch (ResourceAccessException e) {
            throw new ApiException(0, "Сервер %s недоступен".formatted(properties.serverUrl()), null);
        }
    }

    private static ErrorResponse readError(RestClientResponseException e) {
        try {
            return e.getResponseBodyAs(ErrorResponse.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
