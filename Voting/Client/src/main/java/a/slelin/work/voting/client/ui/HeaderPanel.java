package a.slelin.work.voting.client.ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Шапка окна: логотип, адрес сервера, индикатор соединения, участник и переключатель темы.
 */
public class HeaderPanel extends JPanel {

    private final StatusDot dot = new StatusDot();
    private final JLabel connection = Ui.muted("Подключение…", 13f);
    private final JButton voterButton = Ui.ghostButton("");
    private final JButton themeButton = Ui.ghostButton("");

    public HeaderPanel(String serverUrl, Runnable onChangeVoter, Runnable onToggleTheme) {
        super(new BorderLayout());
        setOpaque(false);

        // Слева: логотип и название
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.add(new BallotIcon(40));
        brand.add(Box.createHorizontalStrut(12));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = Ui.label("Голосование", Font.BOLD, 20f);
        JLabel subtitle = Ui.muted("Сервер: " + serverUrl, 12.5f);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(title);
        titles.add(subtitle);
        brand.add(titles);
        add(brand, BorderLayout.WEST);

        // Справа: соединение, участник, тема
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        right.setOpaque(false);
        right.add(dot);
        right.add(connection);
        right.add(Box.createHorizontalStrut(10));

        voterButton.setFocusable(false);
        voterButton.setToolTipText("Сменить участника");
        voterButton.addActionListener(e -> onChangeVoter.run());
        right.add(voterButton);

        themeButton.setFocusable(false);
        themeButton.addActionListener(e -> onToggleTheme.run());
        right.add(themeButton);
        add(right, BorderLayout.EAST);

        updateThemeButton();
    }

    public void setOnline(boolean online) {
        dot.setColor(online ? Theme.SUCCESS : Theme.DANGER);
        connection.setText(online ? "В сети" : "Нет связи");
    }

    public void setVoter(String voter) {
        voterButton.setText("Участник: " + voter);
    }

    public void updateThemeButton() {
        themeButton.setText(Theme.isDark() ? "Светлая тема" : "Тёмная тема");
        connection.setForeground(Theme.muted());
    }
}
