package a.slelin.work.voting.client.ui;

import a.slelin.work.voting.common.dto.PollStatus;
import a.slelin.work.voting.common.dto.PollSummaryResponse;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Левая часть окна: поиск, кнопка создания и список голосований.
 */
public class PollListPanel extends JPanel {

    private final DefaultListModel<PollSummaryResponse> model = new DefaultListModel<>();
    private final JList<PollSummaryResponse> list = new JList<>(model);
    private final JTextField search = new JTextField();
    private final JLabel counter = Ui.muted("", 12.5f);
    private final Consumer<UUID> onSelect;

    private List<PollSummaryResponse> polls = List.of();
    private boolean updating;

    public PollListPanel(Runnable onCreate, Consumer<UUID> onSelect) {
        super(new BorderLayout(0, 12));
        this.onSelect = onSelect;
        setOpaque(false);
        setPreferredSize(new Dimension(330, 0));

        // Заголовок и кнопка «Новое»
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JPanel titleText = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleText.setOpaque(false);
        titleText.add(Ui.label("Голосования", Font.BOLD, 17f));
        titleText.add(Box.createHorizontalStrut(8));
        titleText.add(counter);
        // GridBagLayout по умолчанию центрирует содержимое по вертикали относительно кнопки
        JPanel titleCenter = new JPanel(new GridBagLayout());
        titleCenter.setOpaque(false);
        titleCenter.add(titleText);
        titleRow.add(titleCenter, BorderLayout.WEST);

        JButton create = Ui.primaryButton("+ Новое");
        create.addActionListener(e -> onCreate.run());
        titleRow.add(create, BorderLayout.EAST);

        // Поиск
        search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Поиск по вопросу…");
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        search.putClientProperty(FlatClientProperties.STYLE, "margin: 6,10,6,10");
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(titleRow, BorderLayout.NORTH);
        top.add(search, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // Список
        list.setOpaque(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new PollCard());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !updating && list.getSelectedValue() != null) {
                onSelect.accept(list.getSelectedValue().id());
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Обновляет список, сохраняя выделение и не вызывая повторной загрузки.
     */
    public void setPolls(List<PollSummaryResponse> polls, UUID selectedId) {
        this.polls = polls;
        counter.setText(String.valueOf(polls.size()));
        render(selectedId);
    }

    private void applyFilter() {
        PollSummaryResponse selected = list.getSelectedValue();
        render(selected == null ? null : selected.id());
    }

    private void render(UUID selectedId) {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        List<PollSummaryResponse> visible = new ArrayList<>();
        for (PollSummaryResponse poll : polls) {
            if (query.isEmpty() || poll.question().toLowerCase(Locale.ROOT).contains(query)) {
                visible.add(poll);
            }
        }

        updating = true;
        try {
            model.clear();
            model.addAll(visible);
            list.clearSelection();
            for (int i = 0; i < visible.size(); i++) {
                if (visible.get(i).id().equals(selectedId)) {
                    list.setSelectedIndex(i);
                    list.ensureIndexIsVisible(i);
                    break;
                }
            }
        } finally {
            updating = false;
        }
    }

    /**
     * Карточка голосования в списке.
     */
    private static class PollCard extends JPanel implements ListCellRenderer<PollSummaryResponse> {

        private final JLabel question = Ui.label("", Font.BOLD, 14f);
        private final Badge status = new Badge();
        private final JLabel info = Ui.muted("", 12.5f);
        private boolean selected;

        PollCard() {
            super(new BorderLayout(0, 8));
            setOpaque(false);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            bottom.setOpaque(false);
            bottom.add(status);
            bottom.add(Box.createHorizontalStrut(10));
            bottom.add(info);

            add(question, BorderLayout.NORTH);
            add(bottom, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PollSummaryResponse> list,
                                                      PollSummaryResponse poll, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            selected = isSelected;
            question.setText(poll.question());
            question.setForeground(Theme.text());
            boolean open = poll.status() == PollStatus.OPEN;
            status.set(poll.status().getTitle(), open ? Theme.SUCCESS : Theme.muted());
            info.setText(Ui.votes(poll.totalVotes()) + "  ·  " + Ui.options(poll.optionsCount()));
            info.setForeground(Theme.muted());
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.smooth(g);
            int w = getWidth() - 1;
            int h = getHeight() - 9;
            g2.setColor(selected
                    ? Theme.alpha(Theme.ACCENT, Theme.isDark() ? 0.22 : 0.10)
                    : Theme.surface());
            g2.fillRoundRect(0, 0, w, h, 16, 16);
            g2.setColor(selected ? Theme.ACCENT : Theme.border());
            g2.drawRoundRect(0, 0, w, h, 16, 16);
            if (selected) {
                g2.fillRoundRect(0, 14, 4, h - 28, 4, 4);
            }
            g2.dispose();
        }

        /**
         * Узкая предпочтительная ширина: карточка растягивается по ширине списка, а длинный вопрос обрезается «…».
         * Нижние 9 пикселей — отступ между карточками.
         */
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(120, 85);
        }

        @Override
        public Insets getInsets() {
            return new Insets(13, 18, 22, 16);
        }
    }
}
