package a.slelin.work.voting.client.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Цветная «таблетка» с коротким текстом, например статусом голосования.
 */
public class Badge extends JComponent {

    private String text = "";
    private Color color = Theme.ACCENT;

    public Badge() {
        setFont(Theme.font(Font.BOLD, 11f));
    }

    public void set(String text, Color color) {
        this.text = text;
        this.color = color;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(text) + 22, fm.getHeight() + 6);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth(g);
        int h = getHeight();
        g2.setColor(Theme.alpha(color, Theme.isDark() ? 0.25 : 0.13));
        g2.fillRoundRect(0, 0, getWidth(), h, h, h);

        g2.setColor(color);
        g2.fillOval(8, h / 2 - 3, 6, 6);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Theme.isDark() ? color.brighter() : color.darker());
        g2.drawString(text, 18, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
