package a.slelin.work.mutualtls.client.ui;

import a.slelin.work.mutualtls.client.ClientConfig;
import a.slelin.work.mutualtls.client.connection.ConnectionState;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;

/**
 * Карточка подключения: адрес сервера, папка сертификатов, кнопки Connect/Reconnect и Disconnect,
 * а под ними — строка с подробностями о текущем состоянии.
 */
public class ConnectionPanel extends RoundedPanel {

    private final JTextField host = new JTextField(14);
    private final JSpinner port = new JSpinner(new SpinnerNumberModel(8443, 1, 65535, 1));
    private final JTextField certs = new JTextField(20);
    private final JButton browse = Ui.ghostButton("Обзор…");
    private final JButton connect = Ui.primaryButton("Подключиться");
    private final JButton disconnect = Ui.outlineButton("Отключиться", Theme.DANGER);
    private final StatusDot dot = new StatusDot();
    private final JLabel details = Ui.muted(" ", 12.5f);

    public ConnectionPanel(ClientConfig config, Runnable onConnect, Runnable onDisconnect, Runnable onCertsChanged) {
        super(new BorderLayout(0, 10), 18);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));

        host.setText(config.host());
        port.setValue(config.port());
        port.setEditor(new JSpinner.NumberEditor(port, "#"));
        certs.setText(config.certs().toAbsolutePath().normalize().toString());
        host.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "localhost");
        certs.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "папка с client.pem, client.key, ca.pem");
        // GridBagLayout при нехватке места сжимает поля до минимального размера — не даём хосту и порту «схлопнуться»
        host.setMinimumSize(host.getPreferredSize());
        port.setMinimumSize(port.getPreferredSize());
        certs.setMinimumSize(new Dimension(160, certs.getPreferredSize().height));

        // Верхняя строка: поля ввода и кнопки
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 8);
        c.anchor = GridBagConstraints.WEST;

        fields.add(caption("Хост"), c);
        fields.add(host, c);
        fields.add(caption("Порт"), c);
        fields.add(port, c);
        fields.add(caption("Сертификаты"), c);
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        fields.add(certs, c);
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        fields.add(browse, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(disconnect);
        buttons.add(connect);

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(fields, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.EAST);
        add(top, BorderLayout.CENTER);

        // Нижняя строка: подробности состояния
        JPanel status = new JPanel(new BorderLayout(8, 0));
        status.setOpaque(false);
        status.add(dot, BorderLayout.WEST);
        status.add(details, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        connect.addActionListener(e -> onConnect.run());
        disconnect.addActionListener(e -> onDisconnect.run());
        certs.addActionListener(e -> onCertsChanged.run());
        browse.addActionListener(e -> chooseFolder(onCertsChanged));
    }

    public String host() {
        String value = host.getText().strip();
        return value.isEmpty() ? "localhost" : value;
    }

    public int port() {
        return (Integer) port.getValue();
    }

    public Path certsDirectory() {
        return Path.of(certs.getText().strip());
    }

    /**
     * Переключает кнопки под состояние. Пока соединение установлено (или было), основная кнопка — «Reconnect».
     */
    public void setState(ConnectionState state, String message) {
        boolean busy = state == ConnectionState.CONNECTING;
        connect.setEnabled(!busy);
        connect.setText(state == ConnectionState.DISCONNECTED ? "Подключиться" : "Переподключиться");
        connect.setToolTipText(state == ConnectionState.DISCONNECTED ? "Connect" : "Reconnect");
        disconnect.setEnabled(state == ConnectionState.CONNECTED);
        host.setEnabled(!busy);
        port.setEnabled(!busy);
        certs.setEnabled(!busy);
        browse.setEnabled(!busy);

        dot.setColor(HeaderPanel.colorOf(state));
        details.setText(message);
        details.setForeground(state == ConnectionState.FAILED || state == ConnectionState.LOST
                ? Theme.DANGER : Theme.muted());
    }

    private void chooseFolder(Runnable onCertsChanged) {
        JFileChooser chooser = new JFileChooser(certsDirectory().toFile());
        chooser.setDialogTitle("Папка с сертификатами");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            certs.setText(chooser.getSelectedFile().getAbsolutePath());
            onCertsChanged.run();
        }
    }

    private static JLabel caption(String text) {
        JLabel label = Ui.label(text, Font.BOLD, 12.5f);
        label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        return label;
    }
}
