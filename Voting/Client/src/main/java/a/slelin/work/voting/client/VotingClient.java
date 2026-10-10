package a.slelin.work.voting.client;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VotingClient {

    public static void main(String[] args) {
        new SpringApplicationBuilder(VotingClient.class)
                .web(WebApplicationType.NONE)
                .headless(false)
                .run(args);
    }
}
