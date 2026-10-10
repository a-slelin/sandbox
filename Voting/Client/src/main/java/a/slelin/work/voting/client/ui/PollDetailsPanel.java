package a.slelin.work.voting.client.ui;

import a.slelin.work.voting.common.dto.OptionResponse;
import a.slelin.work.voting.common.dto.PollResponse;
import a.slelin.work.voting.common.dto.PollStatus;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Правая часть окна: выбранное голосование с вариантами ответа и действиями.
 */
public class PollDetailsPanel extends RoundedPanel {

    public interface Listener {

        void onVote(UUID pollId, long optionId);

        void onClose(UUID pollId);

        void onDelete(UUID pollId);
    }

    private static final String EMPTY = "empty";
    private static final String POLL = "poll";

    private final Listener listener;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    private final Badge statusBadge = new Badge();
    private final JLabel meta = Ui.muted("", 12.5f);
    private final JTextArea question = Ui.wrappingText(Font.BOLD, 22f);
    private final JTextArea description = Ui.wrappingText(Font.PLAIN, 14f);
    private final JPanel optionsBox = new JPanel();
    private final JLabel hint = Ui.muted("", 13f);
    private final JButton closeButton = Ui.outlineButton("Завершить", Theme.WARNING);
    private final JButton deleteButton = Ui.outlineButton("Удалить", Theme.DANGER);

    private final List<OptionBar> bars = new ArrayList<>();
    private PollResponse poll;

    public PollDetailsPanel(Listener listener) {
        super(new BorderLayout(), 20);
        this.listener = listener;

        content.setOpaque(false);
        content.add(buildEmpty(), EMPTY);
        content.add(buildPoll(), POLL);
        add(content, BorderLayout.CENTER);

        closeButton.addActionListener(e -> {
            if (poll != null) {
                listener.onClose(poll.id());
            }
        });
        deleteButton.addActionListener(e -> {
            if (poll != null) {
                listener.onDelete(poll.id());
            }
        });

        showEmpty();
    }

    public Optional<UUID> currentId() {
        return Optional.ofNullable(poll).map(PollResponse::id);
    }

    public void showEmpty() {
        poll = null;
        cards.show(content, EMPTY);
    }

    /**
     * Показывает голосование. Если это то же голосование, что уже открыто, полосы обновляются плавно.
     */
    public void show(PollResponse newPoll) {
        boolean samePoll = poll != null && poll.id().equals(newPoll.id())
                && bars.size() == newPoll.options().size();
        poll = newPoll;

        statusBadge.set(newPoll.status().getTitle(),
                newPoll.status() == PollStatus.OPEN ? Theme.SUCCESS : Theme.muted());
        meta.setText(metaText(newPoll));
        question.setText(newPoll.question());
        description.setText(newPoll.description() == null ? "" : newPoll.description());
        description.setVisible(newPoll.description() != null);
        description.setForeground(Theme.muted());

        if (!samePoll) {
            rebuildBars(newPoll);
        }

        boolean votable = newPoll.acceptsVotes() && !newPoll.hasVoted();
        Long winnerId = newPoll.acceptsVotes() ? null
                : newPoll.leader().map(OptionResponse::id).orElse(null);
        for (int i = 0; i < bars.size(); i++) {
            OptionResponse option = newPoll.options().get(i);
            bars.get(i).update(option, votable,
                    Long.valueOf(option.id()).equals(newPoll.votedOptionId()),
                    Long.valueOf(option.id()).equals(winnerId));
        }

        hint.setText(hintText(newPoll));
        closeButton.setVisible(newPoll.acceptsVotes());
        cards.show(content, POLL);
        revalidate();
        repaint();
    }

    private void rebuildBars(PollResponse newPoll) {
        optionsBox.removeAll();
        bars.clear();
        for (int i = 0; i < newPoll.options().size(); i++) {
            OptionBar bar = new OptionBar(i, optionId -> listener.onVote(poll.id(), optionId));
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);
            bars.add(bar);
            optionsBox.add(bar);
        }
        optionsBox.add(Box.createVerticalGlue());
    }

    private JPanel buildEmpty() {
        JPanel empty = new JPanel(new BorderLayout());
        empty.setOpaque(false);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        BallotIcon icon = new BallotIcon(56);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = Ui.label("Выберите голосование", Font.BOLD, 18f);
        JLabel subtitle = Ui.muted("Слева — список голосований. Можно создать новое кнопкой «+ Новое».", 13.5f);
        for (JLabel label : List.of(title, subtitle)) {
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            label.setHorizontalAlignment(SwingConstants.CENTER);
        }

        center.add(Box.createVerticalGlue());
        center.add(icon);
        center.add(Box.createVerticalStrut(16));
        center.add(title);
        center.add(Box.createVerticalStrut(6));
        center.add(subtitle);
        center.add(Box.createVerticalGlue());

        empty.add(center, BorderLayout.CENTER);
        return empty;
    }

    private JPanel buildPoll() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        // Шапка: статус, мета-информация, вопрос и описание
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusRow.setOpaque(false);
        statusRow.add(statusBadge);
        statusRow.add(Box.createHorizontalStrut(12));
        statusRow.add(meta);

        for (JComponent component : List.<JComponent>of(statusRow, question, description)) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        header.add(statusRow);
        header.add(Box.createVerticalStrut(14));
        header.add(question);
        header.add(Box.createVerticalStrut(8));
        header.add(description);
        root.add(header, BorderLayout.NORTH);

        // Варианты ответа
        optionsBox.setOpaque(false);
        optionsBox.setLayout(new BoxLayout(optionsBox, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(optionsBox);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        // Подвал: подсказка и действия
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.add(hint, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(closeButton);
        actions.add(deleteButton);
        footer.add(actions, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        return root;
    }

    private static String metaText(PollResponse poll) {
        String text = "%s  ·  %s  ·  создано %s".formatted(
                Ui.votes(poll.totalVotes()), Ui.options(poll.options().size()), Ui.formatTime(poll.createdAt()));
        return poll.closedAt() == null ? text : text + "  ·  завершено " + Ui.formatTime(poll.closedAt());
    }

    private static String hintText(PollResponse poll) {
        if (!poll.acceptsVotes()) {
            return poll.leader()
                    .map(o -> "Голосование завершено. Победитель — «%s» (%s)".formatted(o.text(), Ui.votes(o.votes())))
                    .orElse("Голосование завершено без голосов");
        }
        if (poll.hasVoted()) {
            return "Ваш голос учтён. Результаты обновляются автоматически";
        }
        return "Нажмите на вариант, чтобы проголосовать";
    }
}
