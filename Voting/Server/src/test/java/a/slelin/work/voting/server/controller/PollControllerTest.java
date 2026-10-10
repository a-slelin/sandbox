package a.slelin.work.voting.server.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "voting.demo-data=false")
@AutoConfigureMockMvc
class PollControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void fullVotingFlow() throws Exception {
        String id = createPoll("""
                {"question": "Tea or coffee?", "options": ["Tea", "Coffee"]}
                """);

        assertThat(vote(id, "Anna", 2)).hasStatusOk()
                .bodyJson().satisfies(json -> {
                    json.assertThat().extractingPath("$.totalVotes").isEqualTo(1);
                    json.assertThat().extractingPath("$.votedOptionId").isEqualTo(2);
                    json.assertThat().extractingPath("$.options[1].percent").isEqualTo(100.0);
                });

        assertThat(vote(id, "anna", 1)).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.status").isEqualTo(409);

        assertThat(mvc.get().uri("/api/polls/{id}?voter=Anna", id)).hasStatusOk()
                .bodyJson().extractingPath("$.votedOptionId").isEqualTo(2);

        assertThat(mvc.post().uri("/api/polls/{id}/close", id)).hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("CLOSED");

        assertThat(vote(id, "Boris", 1)).hasStatus(HttpStatus.CONFLICT);

        assertThat(mvc.get().uri("/api/polls")).hasStatusOk()
                .bodyJson().extractingPath("$[0].totalVotes").isEqualTo(1);

        assertThat(mvc.delete().uri("/api/polls/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/polls/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidPollIsRejectedWithDetails() {
        assertThat(mvc.post().uri("/api/polls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"question": " ", "options": ["Only one"]}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().satisfies(json -> {
                    json.assertThat().extractingPath("$.details.question").isNotNull();
                    json.assertThat().extractingPath("$.details.options").isNotNull();
                });
    }

    @Test
    void unknownPollAndMalformedIdAreHandled() {
        assertThat(mvc.get().uri("/api/polls/{id}", UUID.randomUUID())).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.message").isNotNull();
        assertThat(mvc.get().uri("/api/polls/not-a-uuid")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private String createPoll(String body) throws Exception {
        MvcTestResult result = mvc.post().uri("/api/polls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED).containsHeader("Location");

        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
    }

    private MvcTestResult vote(String pollId, String voter, long optionId) {
        return mvc.post().uri("/api/polls/{id}/votes", pollId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"voter": "%s", "optionId": %d}
                        """.formatted(voter, optionId))
                .exchange();
    }
}
