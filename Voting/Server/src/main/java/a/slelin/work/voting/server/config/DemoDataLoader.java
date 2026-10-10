package a.slelin.work.voting.server.config;

import a.slelin.work.voting.common.dto.PollCreateRequest;
import a.slelin.work.voting.common.dto.PollResponse;
import a.slelin.work.voting.common.dto.VoteRequest;
import a.slelin.work.voting.server.service.PollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Наполняет базу демонстрационными голосованиями, чтобы клиенту сразу было что показать.
 * Данные проходят через {@link PollService}, то есть через те же правила, что и запросы клиентов.
 * Отключается свойством {@code voting.demo-data=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voting.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements ApplicationRunner {

    private final PollService service;

    @Override
    public void run(ApplicationArguments args) {
        PollResponse ide = service.create(new PollCreateRequest("Лучшая IDE для Java",
                null,
                List.of("IntelliJ IDEA", "Eclipse", "VS Code", "NetBeans")));
        vote(ide, 0, "Анна", "Борис", "Вика", "Глеб", "Дима", "Егор");
        vote(ide, 2, "Женя", "Зоя");
        vote(ide, 1, "Иван");
        service.close(ide.id(), null);

        PollResponse language = service.create(new PollCreateRequest(
                "Какой язык программирования изучить следующим?",
                "Выбираем язык для следующего пет-проекта в песочнице.",
                List.of("Kotlin", "Go", "Rust", "Python", "C++")));
        vote(language, 0, "Анна", "Борис", "Вика", "Глеб");
        vote(language, 2, "Дима", "Егор", "Женя");
        vote(language, 1, "Зоя", "Иван");
        vote(language, 3, "Кира");

        PollResponse lunch = service.create(new PollCreateRequest("Куда идём обедать в пятницу?",
                "Голосование закроется в четверг вечером.",
                List.of("Пиццерия", "Суши-бар", "Грузинская кухня", "Столовая")));
        vote(lunch, 2, "Борис", "Кира");
        vote(lunch, 0, "Анна");

        log.info("Demo data loaded: {} polls", service.findAll().size());
    }

    private void vote(PollResponse poll, int optionIndex, String... voters) {
        long optionId = poll.options().get(optionIndex).id();
        for (String voter : voters) {
            service.vote(poll.id(), new VoteRequest(voter, optionId));
        }
    }
}
