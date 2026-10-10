package a.slelin.work.voting.client.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Индикатор соединения с сервером: зелёная или красная точка с мягким ореолом.
 */
public class StatusDot extends JComponent {

    private Color color = Theme.WARNING;

    public StatusDot() {
        setPreferredSize(new Dimension(14, 14));
    }

    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth(g);
        int size = Math.min(getWidth(), getHeight());
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        g2.setColor(Theme.alpha(color, 0.25));
        g2.fillOval(x, y, size, size);
        g2.setColor(color);
        g2.fillOval(x + 3, y + 3, size - 6, size - 6);
        g2.dispose();
    }
}
