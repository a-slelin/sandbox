package a.slelin.work.voting.client.ui;

import a.slelin.work.voting.client.api.VotingApi;
import a.slelin.work.voting.client.config.ClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.swing.SwingUtilities;

/**
 * Запускает Swing-интерфейс после старта Spring-контекста.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UiLauncher implements ApplicationRunner {

    private final VotingApi api;
    private final ClientProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        SwingUtilities.invokeLater(() -> {
            Theme.apply(properties.darkTheme());

            String voter = properties.voter().isBlank()
                    ? MainFrame.askVoter(null, System.getProperty("user.name", ""))
                    : properties.voter().trim();
            if (voter == null) {
                log.info("Voter name was not provided, exiting");
                System.exit(0);
            }

            log.info("Starting UI for voter '{}', server {}", voter, properties.serverUrl());
            new MainFrame(api, voter, properties.refreshInterval()).setVisible(true);
        });
    }
}
