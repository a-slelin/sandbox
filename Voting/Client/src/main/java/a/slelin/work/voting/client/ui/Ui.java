package a.slelin.work.voting.client.ui;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Мелкие помощники для построения интерфейса.
 */
public final class Ui {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private Ui() {
    }

    public static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    public static JLabel label(String text, int style, float size) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(style, size));
        return label;
    }

    /**
     * Приглушённая подпись. Цвет пересчитывается при смене темы.
     */
    public static JLabel muted(String text, float size) {
        JLabel label = new JLabel(text) {
            @Override
            public void updateUI() {
                super.updateUI();
                setForeground(Theme.muted());
            }
        };
        label.setFont(Theme.font(Font.PLAIN, size));
        return label;
    }

    /**
     * Многострочный текст, который переносится по ширине контейнера.
     */
    public static JTextArea wrappingText(int style, float size) {
        JTextArea area = new JTextArea();
        area.setFont(Theme.font(style, size));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder());
        return area;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.putClientProperty(FlatClientProperties.STYLE, """
                background: %s; foreground: #FFFFFF; hoverBackground: %s; pressedBackground: %s;
                borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; font: bold; margin: 7,16,7,16"""
                .formatted(Theme.hex(Theme.ACCENT),
                        Theme.hex(Theme.ACCENT.brighter()),
                        Theme.hex(Theme.ACCENT.darker())));
        return button;
    }

    /**
     * Кнопка-контур цвета {@code color}. Фон совпадает с карточкой и пересчитывается при смене темы.
     */
    public static JButton outlineButton(String text, Color color) {
        return new JButton(text) {
            @Override
            public void updateUI() {
                putClientProperty(FlatClientProperties.STYLE, """
                        background: %1$s; foreground: %2$s; borderColor: %2$s88; hoverBorderColor: %2$s;
                        hoverBackground: %3$s; pressedBackground: %4$s; borderWidth: 1; focusWidth: 0; margin: 7,14,7,14"""
                        .formatted(Theme.hex(Theme.surface()), Theme.hex(color),
                                Theme.hex(Theme.mix(Theme.surface(), color, 0.08)),
                                Theme.hex(Theme.mix(Theme.surface(), color, 0.16))));
                super.updateUI();
            }
        };
    }

    public static JButton ghostButton(String text) {
        JButton button = new JButton(text);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        return button;
    }

    public static String formatTime(Instant instant) {
        return instant == null ? "—" : DATE_TIME.format(instant);
    }

    /**
     * Склонение: 1 голос, 2 голоса, 5 голосов.
     */
    public static String plural(long n, String one, String few, String many) {
        long mod100 = Math.abs(n) % 100;
        long mod10 = mod100 % 10;
        String word = mod100 >= 11 && mod100 <= 14 ? many
                : mod10 == 1 ? one
                : mod10 >= 2 && mod10 <= 4 ? few
                : many;
        return n + " " + word;
    }

    public static String votes(long n) {
        return plural(n, "голос", "голоса", "голосов");
    }

    public static String options(long n) {
        return plural(n, "вариант", "варианта", "вариантов");
    }
}
