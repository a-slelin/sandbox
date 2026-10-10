package a.slelin.work.voting.server.controller;

import a.slelin.work.voting.common.dto.PollCreateRequest;
import a.slelin.work.voting.common.dto.PollResponse;
import a.slelin.work.voting.common.dto.PollSummaryResponse;
import a.slelin.work.voting.common.dto.VoteRequest;
import a.slelin.work.voting.server.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService service;

    @GetMapping
    public List<PollSummaryResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PollResponse get(@PathVariable UUID id,
                            @RequestParam(required = false) String voter) {
        return service.get(id, voter);
    }

    @PostMapping
    public ResponseEntity<PollResponse> create(@Valid @RequestBody PollCreateRequest request) {
        PollResponse poll = service.create(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}").buildAndExpand(poll.id()).toUri())
                .body(poll);
    }

    @PostMapping("/{id}/votes")
    public PollResponse vote(@PathVariable UUID id,
                             @Valid @RequestBody VoteRequest request) {
        return service.vote(id, request);
    }

    @PostMapping("/{id}/close")
    public PollResponse close(@PathVariable UUID id,
                              @RequestParam(required = false) String voter) {
        return service.close(id, voter);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
