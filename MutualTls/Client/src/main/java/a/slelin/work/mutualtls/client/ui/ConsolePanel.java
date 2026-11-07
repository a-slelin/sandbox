package a.slelin.work.mutualtls.client.ui;

import a.slelin.work.mutualtls.common.Protocol;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Журнал событий и обмен командами с сервером по защищённому каналу.
 */
public class ConsolePanel extends RoundedPanel {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Вид строки журнала. */
    public enum Kind {
        /** Событие клиента (подключение, отключение). */
        INFO,
        /** Отправлено серверу. */
        SENT,
        /** Получено от сервера. */
        RECEIVED,
        /** Ошибка. */
        ERROR
    }

    private final JTextPane log = new JTextPane();
    private final JTextField input = new JTextField();
    private final JButton send = Ui.primaryButton("Отправить");
    private final JButton[] quick;

    public ConsolePanel(Consumer<String> onSend) {
        super(new BorderLayout(0, 10), 18);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Заголовок и быстрые команды
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Ui.label("Обмен по защищённому каналу", Font.BOLD, 15f), BorderLayout.WEST);
        JPanel commands = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        commands.setOpaque(false);
        quick = new JButton[]{Ui.outlineButton(Protocol.PING, Theme.ACCENT), Ui.outlineButton(Protocol.WHOAMI, Theme.ACCENT),
                Ui.outlineButton(Protocol.TIME, Theme.ACCENT)};
        for (JButton button : quick) {
            button.setFocusable(false);
            button.addActionListener(e -> onSend.accept(button.getText()));
            commands.add(button);
        }
        header.add(commands, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Журнал
        log.setEditable(false);
        log.setFont(Theme.mono(12.5f));
        JScrollPane scroll = new JScrollPane(log) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createLineBorder(Theme.border()));
                getViewport().getView().setBackground(Theme.surfaceAlt());
            }
        };
        scroll.updateUI();
        add(scroll, BorderLayout.CENTER);

        // Поле ввода
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        input.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Команда или любой текст (сервер ответит эхом)…");
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(send, BorderLayout.EAST);
        add(inputRow, BorderLayout.SOUTH);

        Runnable submit = () -> {
            String text = input.getText().strip();
            if (!text.isEmpty()) {
                onSend.accept(text);
                input.setText("");
            }
        };
        input.addActionListener(e -> submit.run());
        send.addActionListener(e -> submit.run());

        setConnected(false);
    }

    /** Команды можно отправлять только при установленном соединении. */
    public void setConnected(boolean connected) {
        input.setEnabled(connected);
        send.setEnabled(connected);
        for (JButton button : quick) {
            button.setEnabled(connected);
        }
        if (connected) {
            input.requestFocusInWindow();
        }
    }

    /**
     * Добавляет строку в журнал: время, направление и текст.
     */
    public void append(Kind kind, String text) {
        String arrow = switch (kind) {
            case INFO -> "•";
            case SENT -> "→";
            case RECEIVED -> "←";
            case ERROR -> "✕";
        };
        Color color = switch (kind) {
            case INFO -> Theme.muted();
            case SENT -> Theme.ACCENT;
            case RECEIVED -> Theme.SUCCESS;
            case ERROR -> Theme.DANGER;
        };

        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, kind != Kind.INFO);
        StyledDocument doc = log.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), "%s %s %s%n".formatted(TIME.format(LocalTime.now()), arrow, text), style);
        } catch (BadLocationException ignored) {
            // Вставка в конец документа не может выйти за его границы
        }
        log.setCaretPosition(doc.getLength());
    }
}
