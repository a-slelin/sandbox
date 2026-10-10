package a.slelin.work.voting.client.ui;

import a.slelin.work.voting.client.api.ApiException;
import a.slelin.work.voting.client.api.VotingApi;
import a.slelin.work.voting.common.dto.PollResponse;
import a.slelin.work.voting.common.dto.PollSummaryResponse;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Главное окно. Связывает панели между собой и обращается к серверу через {@link VotingApi}.
 * Результаты периодически обновляются, поэтому голоса из других клиентов видны почти сразу.
 */
public class MainFrame extends JFrame implements PollDetailsPanel.Listener {

    private final VotingApi api;
    private final HeaderPanel header;
    private final PollListPanel pollList;
    private final PollDetailsPanel details;
    private final StatusBar statusBar = new StatusBar();
    private final Timer refreshTimer;

    private String voter;
    private UUID selectedId;
    private boolean refreshing;

    public MainFrame(VotingApi api, String voter, Duration refreshInterval) {
        super("Голосование");
        this.api = api;
        this.voter = voter;

        header = new HeaderPanel(api.serverUrl(), this::changeVoter, this::toggleTheme);
        header.setVoter(voter);
        pollList = new PollListPanel(this::createPoll, this::select);
        details = new PollDetailsPanel(this);

        JPanel root = new JPanel(new BorderLayout(20, 18));
        root.setBorder(new EmptyBorder(16, 22, 12, 22));
        root.add(header, BorderLayout.NORTH);
        root.add(pollList, BorderLayout.WEST);
        root.add(details, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);
        setContentPane(root);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setSize(1120, 720);
        setLocationRelativeTo(null);

        refreshTimer = new Timer((int) refreshInterval.toMillis(), e -> refresh());
        refreshTimer.start();
        refresh();
    }

    // ---------- Загрузка данных ----------

    private record Snapshot(List<PollSummaryResponse> polls, PollResponse selected) {
    }

    /**
     * Фоновое обновление: список голосований и выбранное голосование.
     */
    private void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        UUID id = selectedId;
        String currentVoter = voter;

        Async.run(() -> {
                    List<PollSummaryResponse> polls = api.findAll();
                    boolean exists = id != null && polls.stream().anyMatch(p -> p.id().equals(id));
                    return new Snapshot(polls, exists ? api.get(id, currentVoter) : null);
                },
                snapshot -> {
                    refreshing = false;
                    header.setOnline(true);
                    // Пока шёл запрос, пользователь мог выбрать другое голосование
                    if (!Objects.equals(id, selectedId)) {
                        pollList.setPolls(snapshot.polls(), selectedId);
                        return;
                    }
                    if (id != null && snapshot.selected() == null) {
                        selectedId = null;
                        details.showEmpty();
                        statusBar.info("Голосование было удалено");
                    }
                    pollList.setPolls(snapshot.polls(), selectedId);
                    if (snapshot.selected() != null) {
                        details.show(snapshot.selected());
                    }
                },
                failure -> {
                    refreshing = false;
                    handleError(failure);
                });
    }

    private void select(UUID id) {
        selectedId = id;
        Async.run(() -> api.get(id, voter),
                poll -> {
                    if (id.equals(selectedId)) {
                        details.show(poll);
                    }
                },
                this::handleError);
    }

    // ---------- Действия пользователя ----------

    @Override
    public void onVote(UUID pollId, long optionId) {
        Async.run(() -> api.vote(pollId, voter, optionId),
                poll -> {
                    details.show(poll);
                    poll.options().stream()
                            .filter(o -> o.id() == optionId)
                            .findFirst()
                            .ifPresent(o -> statusBar.success("Голос за «%s» учтён".formatted(o.text())));
                    refresh();
                },
                this::handleError);
    }

    @Override
    public void onClose(UUID pollId) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Завершить голосование? После этого голосовать будет нельзя.",
                "Завершение голосования", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        Async.run(() -> api.close(pollId, voter),
                poll -> {
                    details.show(poll);
                    statusBar.success("Голосование завершено");
                    refresh();
                },
                this::handleError);
    }

    @Override
    public void onDelete(UUID pollId) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Удалить голосование вместе со всеми голосами?",
                "Удаление голосования", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        Async.run(() -> {
                    api.delete(pollId);
                    return pollId;
                },
                deleted -> {
                    selectedId = null;
                    details.showEmpty();
                    statusBar.success("Голосование удалено");
                    refresh();
                },
                this::handleError);
    }

    private void createPoll() {
        new CreatePollDialog(this, api).showDialog().ifPresent(poll -> {
            selectedId = poll.id();
            details.show(poll);
            statusBar.success("Голосование «%s» создано".formatted(poll.question()));
            refresh();
        });
    }

    private void changeVoter() {
        String name = askVoter(this, voter);
        if (name != null && !name.equals(voter)) {
            voter = name;
            header.setVoter(name);
            statusBar.info("Теперь вы голосуете как «%s»".formatted(name));
            if (selectedId != null) {
                select(selectedId);
            }
        }
    }

    private void toggleTheme() {
        Theme.toggle();
        header.updateThemeButton();
        SwingUtilities.updateComponentTreeUI(this);
        // Перерисовать карточку голосования новыми цветами, не дожидаясь таймера
        refresh();
    }

    private void handleError(Throwable failure) {
        if (failure instanceof ApiException e) {
            header.setOnline(!e.isConnectionError());
            statusBar.error(e.userMessage());
            if (e.isNotFound()) {
                refresh();
            }
        } else {
            statusBar.error("Ошибка: " + failure.getMessage());
        }
    }

    /**
     * Спрашивает имя участника. Возвращает {@code null}, если пользователь отказался.
     */
    public static String askVoter(Component parent, String current) {
        while (true) {
            Object input = JOptionPane.showInputDialog(parent,
                    "Под этим именем будут учитываться ваши голоса.\nОдин участник — один голос в каждом голосовании.",
                    "Представьтесь", JOptionPane.PLAIN_MESSAGE, null, null, current);
            if (input == null) {
                return null;
            }
            String name = input.toString().trim();
            if (!name.isEmpty() && name.length() <= 50) {
                return name;
            }
            JOptionPane.showMessageDialog(parent, "Имя должно быть от 1 до 50 символов",
                    "Некорректное имя", JOptionPane.WARNING_MESSAGE);
        }
    }
}
