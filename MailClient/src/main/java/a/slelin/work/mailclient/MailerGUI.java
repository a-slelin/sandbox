package a.slelin.work.mailclient;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.regex.Pattern;

public class MailerGUI extends JFrame {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final ConfigManager config;
    private final EmailSender sender;

    private JTextField toField;
    private JTextField subjectField;
    private JTextArea bodyArea;
    private JButton sendButton;
    private JLabel statusLabel;

    public MailerGUI(ConfigManager config) {
        this.config = config;
        this.sender = new EmailSender(config);

        setTitle("Почтовый клиент");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 640);
        setMinimumSize(new Dimension(520, 540));
        setLocationRelativeTo(null);

        setContentPane(buildTabs());
    }

    private JComponent buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("Отправить", buildSendPanel());
        tabs.addTab("История", new HistoryPanel(config));
        return tabs;
    }

    private JComponent buildSendPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.setBackground(Color.WHITE);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        return root;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(new Color(0xF3F6FB));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDCE4F0), 1, true),
                new EmptyBorder(14, 18, 14, 18)
        ));

        JLabel title = new JLabel("Отправка письма");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel from = new JLabel("Отправитель:  " + config.getUser());
        from.setFont(new Font("SansSerif", Font.PLAIN, 13));
        from.setForeground(Color.BLACK);
        from.setAlignmentX(Component.LEFT_ALIGNMENT);
        from.setBorder(new EmptyBorder(8, 0, 2, 0));

        JLabel server = new JLabel("Сервер:  " + config.getSmtpHost() + " : " + config.getImapPort());
        server.setFont(new Font("SansSerif", Font.PLAIN, 13));
        server.setForeground(new Color(0x505A6B));
        server.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(from);
        header.add(server);
        return header;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;

        Font labelFont = new Font("SansSerif", Font.BOLD, 13);
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 14);

        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        JLabel toLabel = new JLabel("Кому");
        toLabel.setFont(labelFont);
        form.add(toLabel, gbc);

        gbc.gridy = 1;
        toField = new JTextField();
        toField.setFont(fieldFont);
        toField.setBorder(fieldBorder());
        form.add(toField, gbc);

        gbc.gridy = 2;
        JLabel subjLabel = new JLabel("Тема");
        subjLabel.setFont(labelFont);
        form.add(subjLabel, gbc);

        gbc.gridy = 3;
        subjectField = new JTextField();
        subjectField.setFont(fieldFont);
        subjectField.setBorder(fieldBorder());
        form.add(subjectField, gbc);

        gbc.gridy = 4;
        JLabel bodyLabel = new JLabel("Сообщение");
        bodyLabel.setFont(labelFont);
        form.add(bodyLabel, gbc);

        gbc.gridy = 5;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        bodyArea = new JTextArea();
        bodyArea.setFont(fieldFont);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane scroll = new JScrollPane(bodyArea);
        scroll.setBorder(fieldBorder());
        form.add(scroll, gbc);

        return form;
    }

    private Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD0D7E2), 1, true),
                new EmptyBorder(6, 10, 6, 10)
        );
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(12, 0, 0, 0));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.add(statusLabel, BorderLayout.CENTER);

        sendButton = new JButton("Отправить");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        sendButton.setBackground(new Color(0x3E7BFA));
        sendButton.setForeground(Color.BLACK);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(new EmptyBorder(10, 28, 10, 28));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> onSend());

        footer.add(sendButton, BorderLayout.EAST);
        return footer;
    }

    private void onSend() {
        String to = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyArea.getText();

        if (to.isEmpty() || !EMAIL_PATTERN.matcher(to).matches()) {
            setStatus("Проверьте адрес получателя", true);
            return;
        }
        if (subject.isEmpty()) {
            setStatus("Введите тему письма", true);
            return;
        }
        if (body.isBlank()) {
            setStatus("Сообщение не может быть пустым", true);
            return;
        }

        sendButton.setEnabled(false);
        setStatus("Отправка...", false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                sender.send(to, subject, body);
                return null;
            }

            @Override
            protected void done() {
                sendButton.setEnabled(true);
                try {
                    get();
                    setStatus("Письмо успешно отправлено", false);
                    toField.setText("");
                    subjectField.setText("");
                    bodyArea.setText("");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    setStatus("Ошибка отправки: " + cause.getMessage(), true);
                }
            }
        };
        worker.execute();
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.setForeground(error ? new Color(0xD03B3B) : new Color(0x2E9E4F));
    }
}