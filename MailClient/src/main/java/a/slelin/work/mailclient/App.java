package a.slelin.work.mailclient;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            try {
                ConfigManager config = new ConfigManager();
                MailerGUI gui = new MailerGUI(config);
                gui.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Не удалось загрузить конфигурацию: " + e.getMessage() +
                                "\n\nУбедитесь, что файл config.properties находится рядом с jar-файлом " +
                                "и содержит поля smtp.host, smtp.port, imap.host, imap.port, email.user, email.password.",
                        "Ошибка запуска",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
