package a.slelin.work.mutualtls.client.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Логотип: замок на скруглённом квадрате. Цвет квадрата отражает состояние соединения.
 */
public class LockIcon extends JComponent {

    private final int size;
    private Color color = Theme.ACCENT;
    private boolean locked;

    public LockIcon(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
    }

    /**
     * @param color  цвет фона
     * @param locked закрытый замок (соединение защищено) или открытый
     */
    public void set(Color color, boolean locked) {
        this.color = color;
        this.locked = locked;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth(g);
        g2.setPaint(new GradientPaint(0, 0, color.brighter(), size, size, color.darker()));
        g2.fillRoundRect(0, 0, size, size, size / 3, size / 3);

        // Корпус замка
        int bodyW = size * 9 / 20;
        int bodyH = size * 7 / 20;
        int bodyX = (size - bodyW) / 2;
        int bodyY = size * 9 / 20;
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(bodyX, bodyY, bodyW, bodyH, size / 8, size / 8);

        // Дужка: у открытого замка смещена вверх
        float stroke = Math.max(2f, size / 12f);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int arcW = bodyW * 2 / 3;
        int arcX = (size - arcW) / 2;
        int arcY = size * 4 / 20 - (locked ? 0 : size / 10);
        g2.drawArc(arcX, arcY, arcW, arcW, 0, 180);
        int legTop = arcY + arcW / 2;
        g2.drawLine(arcX, legTop, arcX, bodyY);
        if (locked) {
            g2.drawLine(arcX + arcW, legTop, arcX + arcW, bodyY);
        }

        // Замочная скважина
        g2.setColor(color.darker());
        int hole = Math.max(3, size / 10);
        g2.fillOval(size / 2 - hole / 2, bodyY + bodyH / 2 - hole / 2, hole, hole);
        g2.dispose();
    }
}
