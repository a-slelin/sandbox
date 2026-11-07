package a.slelin.work.mutualtls.client.ui;

import a.slelin.work.mutualtls.client.connection.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Карточка с параметрами TLS-сессии: протокол, шифр, время рукопожатия и т.д.
 */
public class SessionPanel extends RoundedPanel {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /** Поля в порядке отображения. */
    private final Map<String, JLabel> values = new LinkedHashMap<>();

    public SessionPanel() {
        super(new BorderLayout(0, 10), 18);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        add(Ui.label("TLS-сессия", Font.BOLD, 15f), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 12);
        c.gridy = 0;
        for (String key : new String[]{"Сервер", "Протокол", "Шифр", "Рукопожатие", "Подключено", "ID сессии",
                "Приветствие"}) {
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            grid.add(Ui.muted(key, 12.5f), c);

            JLabel value = Ui.label("—", Font.PLAIN, 13f);
            values.put(key, value);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            grid.add(value, c);
            c.gridy++;
        }
        // Заполнитель, чтобы строки прижались к верху
        c.gridy++;
        c.weighty = 1;
        grid.add(Box.createGlue(), c);
        add(grid, BorderLayout.CENTER);
    }

    public void show(SessionInfo session) {
        set("Сервер", session.host() + ":" + session.port());
        set("Протокол", session.protocol());
        set("Шифр", session.cipherSuite());
        set("Рукопожатие", session.handshake().toMillis() + " мс");
        set("Подключено", TIME.format(session.connectedAt()));
        String id = session.sessionId();
        set("ID сессии", id.length() > 24 ? id.substring(0, 24) + "…" : id);
        values.get("ID сессии").setToolTipText(id);
        set("Приветствие", session.welcome());
    }

    public void clear() {
        values.keySet().forEach(key -> set(key, "—"));
    }

    private void set(String key, String value) {
        JLabel label = values.get(key);
        label.setText(value);
        label.setToolTipText(value);
        label.setFont(Theme.font(key.equals("Протокол") ? Font.BOLD : Font.PLAIN, 13f));
    }
}
