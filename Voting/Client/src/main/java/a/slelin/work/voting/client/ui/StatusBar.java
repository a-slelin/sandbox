package a.slelin.work.voting.client.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;

/**
 * Нижняя строка с сообщениями о результате действий. Сообщение исчезает через несколько секунд.
 */
public class StatusBar extends JPanel {

    private final StatusDot dot = new StatusDot();
    private final JLabel message = Ui.muted(" ", 13f);
    private final Timer hideTimer = new Timer(6000, e -> clear());

    public StatusBar() {
        super(new BorderLayout(8, 0));
        setOpaque(false);
        hideTimer.setRepeats(false);
        dot.setVisible(false);
        add(dot, BorderLayout.WEST);
        add(message, BorderLayout.CENTER);
    }

    public void info(String text) {
        show(text, Theme.ACCENT);
    }

    public void success(String text) {
        show(text, Theme.SUCCESS);
    }

    public void error(String text) {
        show(text, Theme.DANGER);
    }

    private void show(String text, Color color) {
        dot.setColor(color);
        dot.setVisible(true);
        message.setText(text);
        message.setForeground(color == Theme.ACCENT ? Theme.text() : color);
        hideTimer.restart();
    }

    private void clear() {
        dot.setVisible(false);
        message.setText(" ");
    }
}
