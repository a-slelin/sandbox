package a.slelin.work.voting.client.ui;

import a.slelin.work.voting.common.dto.OptionResponse;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.LongConsumer;

/**
 * Вариант ответа: название, количество голосов и анимированная полоса с процентом.
 * Если голосовать можно — вариант кликабельный.
 */
public class OptionBar extends JComponent {

    private static final int HEIGHT = 58;
    private static final int GAP = 10;
    private static final int ARC = 14;

    private final int colorIndex;
    private final LongConsumer onVote;
    private final Timer animation;

    private OptionResponse option;
    private boolean votable;
    private boolean chosen;
    private boolean winner;
    private boolean hover;

    private double shown;
    private double target;

    public OptionBar(int colorIndex, LongConsumer onVote) {
        this.colorIndex = colorIndex;
        this.onVote = onVote;
        this.animation = new Timer(15, e -> step());

        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (votable && option != null) {
                    onVote.accept(option.id());
                }
            }
        });
    }

    public void update(OptionResponse option, boolean votable, boolean chosen, boolean winner) {
        this.option = option;
        this.votable = votable;
        this.chosen = chosen;
        this.winner = winner;
        this.target = option.percent() / 100.0;

        setCursor(votable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        setToolTipText(votable ? "Проголосовать за «%s»".formatted(option.text()) : null);
        if (!animation.isRunning() && Math.abs(target - shown) > 0.0005) {
            animation.start();
        }
        repaint();
    }

    private void step() {
        shown += (target - shown) * 0.12;
        if (Math.abs(target - shown) < 0.002) {
            shown = target;
            animation.stop();
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(320, HEIGHT + GAP);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, HEIGHT + GAP);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (option == null) {
            return;
        }

        Graphics2D g2 = Ui.smooth(g);
        Color color = Theme.option(colorIndex);
        int w = getWidth() - 1;
        int h = HEIGHT;
        Shape card = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, ARC, ARC);

        // Дорожка и заполненная часть
        g2.setColor(Theme.surfaceAlt());
        g2.fill(card);
        if (shown > 0) {
            g2.setClip(card);
            g2.setColor(Theme.alpha(color, Theme.isDark() ? 0.38 : 0.22));
            g2.fillRect(0, 0, (int) Math.round(w * shown), h);
            g2.setClip(null);
        }

        // Рамка: выбранный вариант, наведение или обычная
        if (chosen) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
        } else if (hover && votable) {
            g2.setColor(Theme.alpha(color, 0.8));
            g2.setStroke(new BasicStroke(1.5f));
        } else {
            g2.setColor(Theme.border());
        }
        g2.draw(card);

        // Цветная метка варианта
        g2.setColor(color);
        g2.fillOval(16, h / 2 - 5, 10, 10);

        // Справа: процент и количество голосов
        g2.setFont(Theme.font(Font.BOLD, 16f));
        FontMetrics percentMetrics = g2.getFontMetrics();
        String percent = formatPercent(option.percent());
        int percentX = w - 16 - percentMetrics.stringWidth(percent);
        g2.setColor(Theme.text());
        g2.drawString(percent, percentX, baseline(percentMetrics, h));

        g2.setFont(Theme.font(Font.PLAIN, 12.5f));
        FontMetrics votesMetrics = g2.getFontMetrics();
        String votes = Ui.votes(option.votes());
        int votesX = percentX - 14 - votesMetrics.stringWidth(votes);
        g2.setColor(Theme.muted());
        g2.drawString(votes, votesX, baseline(votesMetrics, h));

        // Слева: текст варианта и пометка «ваш голос» / «победитель»
        int textX = 38;
        String tag = chosen ? "✓ ваш голос" : winner ? "★ победитель" : null;
        Font tagFont = Theme.font(Font.BOLD, 11f);
        int tagWidth = tag == null ? 0 : g2.getFontMetrics(tagFont).stringWidth(tag) + 18;

        g2.setFont(Theme.font(Font.BOLD, 14.5f));
        FontMetrics textMetrics = g2.getFontMetrics();
        int maxTextWidth = votesX - textX - 16 - tagWidth;
        String text = ellipsize(option.text(), textMetrics, maxTextWidth);
        g2.setColor(Theme.text());
        g2.drawString(text, textX, baseline(textMetrics, h));

        if (tag != null) {
            Color tagColor = chosen ? color : Theme.WARNING;
            int tagX = textX + textMetrics.stringWidth(text) + 10;
            FontMetrics tagMetrics = g2.getFontMetrics(tagFont);
            int tagH = tagMetrics.getHeight() + 4;
            int tagY = (h - tagH) / 2;
            g2.setColor(Theme.alpha(tagColor, Theme.isDark() ? 0.3 : 0.16));
            g2.fillRoundRect(tagX, tagY, tagWidth - 8, tagH, tagH, tagH);
            g2.setFont(tagFont);
            g2.setColor(Theme.isDark() ? tagColor.brighter() : tagColor.darker());
            g2.drawString(tag, tagX + 5, tagY + 2 + tagMetrics.getAscent());
        }

        g2.dispose();
    }

    private static int baseline(FontMetrics metrics, int height) {
        return (height - metrics.getHeight()) / 2 + metrics.getAscent();
    }

    private static String formatPercent(double percent) {
        return percent == Math.rint(percent)
                ? "%d%%".formatted((long) percent)
                : "%.1f%%".formatted(percent);
    }

    private static String ellipsize(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String dots = "…";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end) + dots) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + dots;
    }
}
