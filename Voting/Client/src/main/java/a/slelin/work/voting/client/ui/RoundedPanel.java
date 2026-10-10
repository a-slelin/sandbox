package a.slelin.work.voting.client.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.util.function.Supplier;

/**
 * Панель-карточка со скруглёнными углами и тонкой рамкой.
 */
public class RoundedPanel extends JPanel {

    private final int arc;
    private final Supplier<Color> fill;

    public RoundedPanel(LayoutManager layout, int arc) {
        this(layout, arc, Theme::surface);
    }

    public RoundedPanel(LayoutManager layout, int arc, Supplier<Color> fill) {
        super(layout);
        this.arc = arc;
        this.fill = fill;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth(g);
        g2.setColor(fill.get());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.setColor(Theme.border());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
