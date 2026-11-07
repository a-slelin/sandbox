package a.slelin.work.mutualtls.client.ui;

import a.slelin.work.mutualtls.client.connection.ConnectionState;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Шапка окна: логотип-замок, название, статус соединения и переключатель темы.
 */
public class HeaderPanel extends JPanel {

    private final LockIcon logo = new LockIcon(42);
    private final Badge status = new Badge();
    private final JButton themeButton = Ui.ghostButton("");

    public HeaderPanel(Runnable onToggleTheme) {
        super(new BorderLayout());
        setOpaque(false);

        // Слева: логотип и название
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.add(logo);
        brand.add(Box.createHorizontalStrut(12));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = Ui.label("mTLS-клиент", Font.BOLD, 20f);
        JLabel subtitle = Ui.muted("Взаимная TLS-аутентификация по сертификатам X.509", 12.5f);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(title);
        titles.add(subtitle);
        brand.add(titles);
        add(brand, BorderLayout.WEST);

        // Справа: статус соединения и тема
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        right.setOpaque(false);
        status.setToolTipText("Статус соединения");
        right.add(status);

        themeButton.setFocusable(false);
        themeButton.addActionListener(e -> onToggleTheme.run());
        right.add(themeButton);
        add(right, BorderLayout.EAST);

        setState(ConnectionState.DISCONNECTED);
        updateThemeButton();
    }

    public void setState(ConnectionState state) {
        Color color = colorOf(state);
        status.set(state.title(), color);
        logo.set(color, state == ConnectionState.CONNECTED);
    }

    public void updateThemeButton() {
        themeButton.setText(Theme.isDark() ? "☀ Светлая тема" : "☾ Тёмная тема");
    }

    /** Цвет состояния: один и тот же для шапки, строки статуса и логотипа. */
    public static Color colorOf(ConnectionState state) {
        return switch (state) {
            case DISCONNECTED -> Theme.NEUTRAL;
            case CONNECTING -> Theme.WARNING;
            case CONNECTED -> Theme.SUCCESS;
            case FAILED, LOST -> Theme.DANGER;
        };
    }
}
