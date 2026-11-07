package a.slelin.work.mutualtls.client.ui;

import a.slelin.work.mutualtls.common.CertificateInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Карточка одного сертификата: кому и кем выдан, срок действия, ключ, назначение, отпечаток SHA-256.
 * Без сертификата показывает подсказку (например, «появится после рукопожатия»).
 */
public class CertificateCard extends RoundedPanel {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault());

    /** Ширина колонки с названиями полей. */
    private static final int KEY_WIDTH = 104;

    private final JLabel source = Ui.muted(" ", 12f);
    /** Первая строка «таблеток»: срок действия и роль сертификата. */
    private final JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    /** Вторая строка: результаты проверок (FlowLayout не переносит строки внутри BoxLayout, поэтому их две). */
    private final JPanel notes = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JPanel body = new JPanel();

    /**
     * @param title заголовок карточки, например «Клиент · мой сертификат»
     */
    public CertificateCard(String title) {
        super(new BorderLayout(0, 10), 18);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel titleLabel = Ui.label(title, Font.BOLD, 15f);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        source.setAlignmentX(Component.LEFT_ALIGNMENT);
        badges.setOpaque(false);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);
        badges.setBorder(BorderFactory.createEmptyBorder(8, -6, 0, 0));
        notes.setOpaque(false);
        notes.setAlignmentX(Component.LEFT_ALIGNMENT);
        notes.setBorder(BorderFactory.createEmptyBorder(6, -6, 0, 0));
        header.add(titleLabel);
        header.add(source);
        header.add(badges);
        header.add(notes);
        add(header, BorderLayout.NORTH);

        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        add(body, BorderLayout.CENTER);
    }

    /**
     * Показывает сертификат.
     *
     * @param certificate сертификат
     * @param source      откуда он взят (файл или «получен от сервера»)
     * @param checks      дополнительные «таблетки», например «✓ проверен по ca.pem»; начинающиеся с «✕» — красные
     */
    public void show(X509Certificate certificate, String source, List<String> checks) {
        CertificateInfo info = CertificateInfo.of(certificate);
        Instant now = Instant.now();
        this.source.setText(source);

        badges.removeAll();
        boolean valid = info.isValidAt(now);
        badges.add(badge(valid ? "Действителен · ещё " + info.daysLeft(now) + " дн." : "Недействителен",
                valid ? Theme.SUCCESS : Theme.DANGER));
        badges.add(badge(role(info), Theme.ACCENT));
        notes.removeAll();
        for (String check : checks) {
            notes.add(badge(check, check.startsWith("✕") ? Theme.DANGER : Theme.SUCCESS));
        }
        notes.setVisible(!checks.isEmpty());

        body.removeAll();
        row("Кому выдан", info.subjectName(), true);
        row("Группа (OU)", info.subjectUnit(), true);
        row("Организация", info.subjectOrganization(), false);
        if (info.subject().containsKey("EMAIL")) {
            row("E-mail", info.subject().get("EMAIL"), false);
        }
        row("Кем выдан", info.issuerName() + (info.selfSigned() ? " (сам себе)" : ""), false);
        row("Действителен", DATE.format(info.notBefore()) + " — " + DATE.format(info.notAfter()), false);
        row("Серийный №", info.serialNumberGrouped(), false, true);
        row("Ключ / подпись", info.publicKey() + " · " + info.signatureAlgorithm(), false);
        if (!info.alternativeNames().isEmpty()) {
            row("Альт. имена", String.join(", ", info.alternativeNames()), false);
        }
        if (!info.extendedKeyUsage().isEmpty()) {
            row("Назначение", String.join("\n", info.extendedKeyUsage()), false);
        }
        row("Key usage", info.keyUsage().isEmpty() ? "—" : String.join(", ", info.keyUsage()), false);
        row("SHA-256", info.sha256Lines(8), false, true);
        refresh();
    }

    /**
     * Показывает подсказку вместо сертификата.
     */
    public void showPlaceholder(String source, String message, boolean error) {
        this.source.setText(source);
        badges.removeAll();
        badges.add(badge(error ? "Ошибка" : "Нет данных", error ? Theme.DANGER : Theme.NEUTRAL));
        notes.removeAll();
        notes.setVisible(false);

        body.removeAll();
        JTextArea text = Ui.wrappingText(Font.PLAIN, 13f);
        text.setText(message);
        text.setForeground(error ? Theme.DANGER : Theme.muted());
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(Box.createVerticalStrut(6));
        body.add(text);
        refresh();
    }

    private void row(String key, String value, boolean bold) {
        row(key, value, bold, false);
    }

    /**
     * Строка «название — значение». Значение переносится по ширине карточки.
     */
    private void row(String key, String value, boolean bold, boolean mono) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override
            public Dimension getMaximumSize() {
                // BoxLayout растягивает строку на всю ширину, но не по высоте
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        JLabel keyLabel = Ui.muted(key, 12.5f);
        keyLabel.setVerticalAlignment(JLabel.TOP);
        keyLabel.setPreferredSize(new Dimension(KEY_WIDTH, keyLabel.getPreferredSize().height));
        row.add(keyLabel, BorderLayout.WEST);

        JTextArea text = Ui.wrappingText(bold ? Font.BOLD : Font.PLAIN, 13f);
        if (mono) {
            text.setFont(Theme.mono(12f));
        }
        text.setText(value);
        // Значения можно выделить и скопировать (например, отпечаток)
        text.setFocusable(true);
        row.add(text, BorderLayout.CENTER);

        body.add(row);
    }

    private void refresh() {
        body.add(Box.createVerticalGlue());
        revalidate();
        repaint();
    }

    private static Badge badge(String text, Color color) {
        Badge badge = new Badge();
        badge.set(text, color);
        return badge;
    }

    private static String role(CertificateInfo info) {
        if (info.certificateAuthority()) {
            return "Корневой CA";
        }
        String usage = String.join(" ", info.extendedKeyUsage());
        if (usage.contains("serverAuth")) {
            return "TLS-сервер";
        }
        return usage.contains("clientAuth") ? "TLS-клиент" : "Сертификат";
    }
}
