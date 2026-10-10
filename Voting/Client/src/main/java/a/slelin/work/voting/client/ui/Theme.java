package a.slelin.work.voting.client.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.Map;

/**
 * Палитра и шрифты приложения. Цвета берутся в момент отрисовки, поэтому смена темы подхватывается сразу.
 */
public final class Theme {

    public static final Color ACCENT = new Color(0x5B6CFF);
    public static final Color SUCCESS = new Color(0x22A06B);
    public static final Color DANGER = new Color(0xE5484D);
    public static final Color WARNING = new Color(0xF0A020);

    private static final Color[] OPTION_COLORS = {
            new Color(0x5B6CFF), new Color(0x22A06B), new Color(0xF0A020), new Color(0xE5484D),
            new Color(0x8E4EC6), new Color(0x12A5B5), new Color(0xE5679B), new Color(0x7A8699),
            new Color(0xD9822B), new Color(0x3E9BE0)
    };

    private Theme() {
    }

    public static void apply(boolean dark) {
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#5B6CFF"));
        if (dark) {
            FlatMacDarkLaf.setup();
        } else {
            FlatMacLightLaf.setup();
        }

        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("OptionPane.buttonMinimumWidth", 90);
    }

    public static void toggle() {
        apply(!isDark());
        FlatLaf.updateUI();
    }

    public static boolean isDark() {
        return FlatLaf.isLafDark();
    }

    public static Color option(int index) {
        return OPTION_COLORS[Math.floorMod(index, OPTION_COLORS.length)];
    }

    public static Color background() {
        return UIManager.getColor("Panel.background");
    }

    /**
     * Фон карточек.
     */
    public static Color surface() {
        return isDark() ? mix(background(), Color.WHITE, 0.05) : Color.WHITE;
    }

    /**
     * Фон «дорожек» прогресса и полей внутри карточек.
     */
    public static Color surfaceAlt() {
        return isDark() ? mix(background(), Color.WHITE, 0.10) : new Color(0xF2F4F8);
    }

    public static Color border() {
        return isDark() ? mix(background(), Color.WHITE, 0.14) : new Color(0xE1E4EA);
    }

    public static Color text() {
        return UIManager.getColor("Label.foreground");
    }

    public static Color muted() {
        return isDark() ? new Color(0x9BA1AC) : new Color(0x6B7280);
    }

    public static Font font(int style, float size) {
        return UIManager.getFont("Label.font").deriveFont(style, size);
    }

    public static Color alpha(Color color, double alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) Math.round(alpha * 255));
    }

    public static Color mix(Color a, Color b, double ratio) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * ratio),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * ratio),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * ratio));
    }

    public static String hex(Color color) {
        return "#%02X%02X%02X".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }
}
