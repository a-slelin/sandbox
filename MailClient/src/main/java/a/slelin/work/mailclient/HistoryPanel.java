package a.slelin.work.mailclient;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;

public class HistoryPanel extends JPanel {

    private final MailReceiver receiver;
    private final DefaultTableModel model;
    private final JTable table;
    private final JButton refreshButton;
    private final JLabel statusLabel;

    public HistoryPanel(ConfigManager config) {
        this.receiver = new MailReceiver(config);

        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(20, 24, 20, 24));
        setBackground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setBackground(Color.WHITE);

        JLabel title = new JLabel("Получение последних 10 сообщений");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(String.format(
                "Пользователь: %s   •   IMAP: %s:%d",
                config.getUser(), config.getImapHost(), config.getImapPort()));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0x808A9B));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(subtitle);

        top.add(headerText, BorderLayout.WEST);

        refreshButton = new JButton("Обновить");
        refreshButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadHistory());
        top.add(refreshButton, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"Дата", "От кого", "Тема", "Текст"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(300);

        add(new JScrollPane(table), BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);

        loadHistory();
    }

    private void loadHistory() {
        refreshButton.setEnabled(false);
        setStatus("Загрузка...", false);

        SwingWorker<List<MailReceiver.MailInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<MailReceiver.MailInfo> doInBackground() throws Exception {
                return receiver.fetchLast(10);
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                try {
                    List<MailReceiver.MailInfo> messages = get();
                    model.setRowCount(0);
                    for (MailReceiver.MailInfo mi : messages) {
                        model.addRow(new Object[]{mi.date(), mi.from(), mi.subject(), mi.preview()});
                    }
                    setStatus(messages.isEmpty() ? "Писем не найдено" : "Загружено писем: " + messages.size(), false);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    setStatus("Ошибка загрузки: " + cause.getMessage(), true);
                }
            }
        };
        worker.execute();
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.setForeground(error ? new Color(0xD03B3B) : new Color(0x808A9B));
    }
}