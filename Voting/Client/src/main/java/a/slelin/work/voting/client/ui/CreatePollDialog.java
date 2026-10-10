package a.slelin.work.voting.client.ui;

import a.slelin.work.voting.client.api.ApiException;
import a.slelin.work.voting.client.api.VotingApi;
import a.slelin.work.voting.common.dto.PollCreateRequest;
import a.slelin.work.voting.common.dto.PollResponse;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Диалог создания голосования: вопрос, описание и от 2 до 10 вариантов ответа.
 */
public class CreatePollDialog extends JDialog {

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 10;
    private static final int MAX_HEIGHT = 760;

    private final VotingApi api;

    private final JTextField question = new JTextField();
    private final JTextArea description = new JTextArea(3, 20);
    private final JPanel optionsBox = new JPanel();
    private final List<OptionRow> rows = new ArrayList<>();
    private final JButton addOption = Ui.ghostButton("+ Добавить вариант");
    private final JLabel error = Ui.label(" ", Font.PLAIN, 13f);
    private final JButton createButton = Ui.primaryButton("Создать");

    private PollResponse result;

    public CreatePollDialog(JFrame owner, VotingApi api) {
        super(owner, "Новое голосование", true);
        this.api = api;

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(22, 24, 18, 24));
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);

        addRow("");
        addRow("");

        getRootPane().setDefaultButton(createButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setMinimumSize(new Dimension(480, 520));
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Показывает диалог и возвращает созданное голосование, если пользователь его создал.
     */
    public Optional<PollResponse> showDialog() {
        setVisible(true);
        return Optional.ofNullable(result);
    }

    private JComponent buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel title = Ui.label("Новое голосование", Font.BOLD, 20f);
        JLabel subtitle = Ui.muted("Задайте вопрос и от 2 до 10 вариантов ответа", 13f);

        question.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Например: куда идём обедать в пятницу?");
        question.putClientProperty(FlatClientProperties.STYLE, "margin: 7,10,7,10");

        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFont(question.getFont());
        description.putClientProperty(FlatClientProperties.STYLE, "margin: 7,10,7,10");
        JScrollPane descriptionScroll = new JScrollPane(description);

        optionsBox.setLayout(new BoxLayout(optionsBox, BoxLayout.Y_AXIS));
        addOption.addActionListener(e -> {
            OptionRow row = addRow("");
            row.field.requestFocusInWindow();
        });

        error.setForeground(Theme.DANGER);

        List<JComponent> components = List.of(
                title, gap(4), subtitle, gap(18),
                section("Вопрос"), gap(6), question, gap(14),
                optionalSection("Описание", "необязательно"), gap(6), descriptionScroll, gap(14),
                section("Варианты ответа"), gap(6), optionsBox, addOption, gap(8), error);
        for (JComponent component : components) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (component instanceof JTextField || component instanceof JScrollPane) {
                component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
            }
            form.add(component);
        }

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancel = new JButton("Отмена");
        cancel.addActionListener(e -> dispose());
        createButton.addActionListener(e -> submit());
        footer.add(cancel);
        footer.add(createButton);
        return footer;
    }

    private OptionRow addRow(String text) {
        OptionRow row = new OptionRow(rows.size(), text);
        rows.add(row);
        optionsBox.add(row);
        refreshRows();
        return row;
    }

    private void removeRow(OptionRow row) {
        rows.remove(row);
        optionsBox.remove(row);
        refreshRows();
    }

    private void refreshRows() {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setIndex(i);
            rows.get(i).remove.setEnabled(rows.size() > MIN_OPTIONS);
        }
        addOption.setEnabled(rows.size() < MAX_OPTIONS);
        addOption.setText(rows.size() < MAX_OPTIONS ? "+ Добавить вариант" : "Достигнут максимум вариантов");
        optionsBox.revalidate();
        optionsBox.repaint();
        growToContent();
    }

    /**
     * Растягивает открытый диалог под новые варианты; дальше — прокрутка.
     */
    private void growToContent() {
        if (!isDisplayable()) {
            return;
        }
        int height = Math.min(getPreferredSize().height, MAX_HEIGHT);
        if (height > getHeight()) {
            setSize(getWidth(), height);
            validate();
        }
    }

    private void submit() {
        String questionText = question.getText().trim();
        List<String> options = rows.stream()
                .map(row -> row.field.getText().trim())
                .filter(text -> !text.isEmpty())
                .toList();

        String validation = validate(questionText, options);
        if (validation != null) {
            error.setText(validation);
            return;
        }

        error.setText(" ");
        createButton.setEnabled(false);
        createButton.setText("Создание…");

        PollCreateRequest request = new PollCreateRequest(questionText, description.getText().trim(), options);
        Async.run(() -> api.create(request),
                poll -> {
                    result = poll;
                    dispose();
                },
                failure -> {
                    createButton.setEnabled(true);
                    createButton.setText("Создать");
                    error.setText(failure instanceof ApiException apiError ? apiError.userMessage() : failure.getMessage());
                });
    }

    private static String validate(String question, List<String> options) {
        if (question.isEmpty()) {
            return "Введите вопрос";
        }
        if (options.size() < MIN_OPTIONS) {
            return "Нужно хотя бы два варианта ответа";
        }
        Set<String> unique = new HashSet<>();
        for (String option : options) {
            if (!unique.add(option.toLowerCase(Locale.ROOT))) {
                return "Вариант «%s» повторяется".formatted(option);
            }
        }
        return null;
    }

    private static JLabel section(String text) {
        return Ui.label(text, Font.BOLD, 13f);
    }

    private static JComponent optionalSection(String text, String note) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.add(section(text));
        row.add(Box.createHorizontalStrut(6));
        row.add(Ui.muted(note, 12.5f));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JComponent gap(int height) {
        return (JComponent) Box.createVerticalStrut(height);
    }

    /**
     * Строка варианта: цветная метка, поле ввода и кнопка удаления.
     */
    private class OptionRow extends JPanel {

        private final JTextField field = new JTextField();
        private final JButton remove = Ui.ghostButton("✕");
        private final ColorMark mark = new ColorMark();

        OptionRow(int index, String text) {
            super(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(0, 0, 8, 0));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            field.setText(text);
            field.putClientProperty(FlatClientProperties.STYLE, "margin: 6,10,6,10");
            // Enter: перейти к следующему варианту, а в последнем — добавить новый
            field.addActionListener(e -> {
                int position = rows.indexOf(this);
                if (position < rows.size() - 1) {
                    rows.get(position + 1).field.requestFocusInWindow();
                } else if (rows.size() < MAX_OPTIONS && !field.getText().isBlank()) {
                    SwingUtilities.invokeLater(() -> addRow("").field.requestFocusInWindow());
                }
            });
            remove.setToolTipText("Удалить вариант");
            remove.addActionListener(e -> removeRow(this));

            add(mark, BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
            add(remove, BorderLayout.EAST);
            setIndex(index);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
        }

        void setIndex(int index) {
            mark.index = index;
            field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Вариант " + (index + 1));
            mark.repaint();
        }
    }

    private static class ColorMark extends JComponent {

        private int index;

        ColorMark() {
            setPreferredSize(new Dimension(12, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.smooth(g);
            g2.setColor(Theme.option(index));
            g2.fillOval(1, getHeight() / 2 - 5, 10, 10);
            g2.dispose();
        }
    }
}
