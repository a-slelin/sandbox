package a.slelin.work.voting.server.config;

import a.slelin.work.voting.server.model.Poll;
import a.slelin.work.voting.server.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Наполняет сервер демонстрационными голосованиями, чтобы клиенту сразу было что показать.
 * Отключается свойством {@code voting.demo-data=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voting.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements ApplicationRunner {

    private final PollRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        Poll language = new Poll("Какой язык программирования изучить следующим?",
                "Выбираем язык для следующего пет-проекта в песочнице.",
                List.of("Kotlin", "Go", "Rust", "Python", "C++"));
        vote(language, 1, "Анна", "Борис", "Вика", "Глеб");
        vote(language, 3, "Дима", "Егор", "Женя");
        vote(language, 2, "Зоя", "Иван");
        vote(language, 4, "Кира");

        Poll ide = new Poll("Лучшая IDE для Java",
                null,
                List.of("IntelliJ IDEA", "Eclipse", "VS Code", "NetBeans"));
        vote(ide, 1, "Анна", "Борис", "Вика", "Глеб", "Дима", "Егор");
        vote(ide, 3, "Женя", "Зоя");
        vote(ide, 2, "Иван");
        ide.close();

        Poll lunch = new Poll("Куда идём обедать в пятницу?",
                "Голосование закроется в четверг вечером.",
                List.of("Пиццерия", "Суши-бар", "Грузинская кухня", "Столовая"));
        vote(lunch, 3, "Борис", "Кира");
        vote(lunch, 1, "Анна");

        List.of(ide, language, lunch).forEach(repository::save);
        log.info("Demo data loaded: {} polls", repository.findAll().size());
    }

    private static void vote(Poll poll, long optionId, String... voters) {
        for (String voter : voters) {
            poll.vote(voter, optionId);
        }
    }
}
