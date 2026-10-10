package a.slelin.work.voting.client.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * Логотип приложения: скруглённый квадрат с градиентом и галочкой.
 */
public class BallotIcon extends JComponent {

    private final int size;

    public BallotIcon(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth(g);
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        g2.setPaint(new GradientPaint(x, y, Theme.ACCENT.brighter(), x + size, y + size, Theme.option(4)));
        g2.fillRoundRect(x, y, size, size, size / 3, size / 3);

        Path2D check = new Path2D.Double();
        check.moveTo(x + size * 0.28, y + size * 0.52);
        check.lineTo(x + size * 0.44, y + size * 0.68);
        check.lineTo(x + size * 0.73, y + size * 0.34);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(size / 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(check);
        g2.dispose();
    }
}
