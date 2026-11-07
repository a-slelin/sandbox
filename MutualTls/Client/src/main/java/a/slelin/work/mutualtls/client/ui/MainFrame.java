package a.slelin.work.mutualtls.client.ui;

import a.slelin.work.mutualtls.client.ClientConfig;
import a.slelin.work.mutualtls.client.connection.ConnectionErrors;
import a.slelin.work.mutualtls.client.connection.ConnectionState;
import a.slelin.work.mutualtls.client.connection.SessionInfo;
import a.slelin.work.mutualtls.client.connection.TlsConnection;
import a.slelin.work.mutualtls.client.ui.ConsolePanel.Kind;
import a.slelin.work.mutualtls.common.CertificateBundle;
import a.slelin.work.mutualtls.common.CertificateLoadException;
import a.slelin.work.mutualtls.common.CertificatePrinter;
import a.slelin.work.mutualtls.common.Protocol;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Главное окно клиента.
 * <p>
 * Сверху — шапка со статусом и карточка подключения (Connect/Reconnect), в центре — три карточки сертификатов
 * (мой, сервера, CA), снизу — параметры TLS-сессии и консоль для обмена командами.
 * <p>
 * Все изменения интерфейса происходят в потоке Swing (EDT); подключение выполняется в фоне через {@link Async},
 * а события от потока чтения переносятся в EDT через {@link SwingUtilities#invokeLater}.
 */
public class MainFrame extends JFrame implements TlsConnection.Listener {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final HeaderPanel header = new HeaderPanel(this::toggleTheme);
    private final ConnectionPanel connectionPanel;
    private final CertificateCard clientCard = new CertificateCard("Клиент · мой сертификат");
    private final CertificateCard serverCard = new CertificateCard("Сервер · сертификат собеседника");
    private final CertificateCard caCard = new CertificateCard("Корневой CA · доверенный");
    private final SessionPanel sessionPanel = new SessionPanel();
    private final ConsolePanel console = new ConsolePanel(this::send);

    /** Текущий набор сертификатов клиента; {@code null}, если загрузить не удалось. */
    private CertificateBundle bundle;

    /** Текущее соединение; {@code null}, если не подключены. */
    private TlsConnection connection;

    private ConnectionState state = ConnectionState.DISCONNECTED;
    private String stateMessage = "";

    public MainFrame(ClientConfig config) {
        super("mTLS-клиент");
        connectionPanel = new ConnectionPanel(config, this::connect, this::disconnect, this::reloadCertificates);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        header.setAlignmentX(LEFT_ALIGNMENT);
        connectionPanel.setAlignmentX(LEFT_ALIGNMENT);
        top.add(header);
        top.add(Box.createVerticalStrut(14));
        top.add(connectionPanel);

        ScrollablePanel cards = new ScrollablePanel(new GridLayout(1, 3, 16, 0));
        cards.add(clientCard);
        cards.add(serverCard);
        cards.add(caCard);
        // На маленьком экране карточки прокручиваются, а не обрезаются
        JScrollPane cardsScroll = new JScrollPane(cards, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cardsScroll.setBorder(BorderFactory.createEmptyBorder());
        cardsScroll.setOpaque(false);
        cardsScroll.getViewport().setOpaque(false);
        cardsScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel bottom = new JPanel(new BorderLayout(16, 0));
        bottom.setOpaque(false);
        sessionPanel.setPreferredSize(new Dimension(400, 230));
        bottom.add(sessionPanel, BorderLayout.WEST);
        console.setPreferredSize(new Dimension(600, 230));
        bottom.add(console, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(16, 22, 18, 22));
        root.add(top, BorderLayout.NORTH);
        root.add(cardsScroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeConnection();
            }
        });
        setMinimumSize(new Dimension(1180, 760));
        setSize(1320, 900);
        setLocationRelativeTo(null);

        serverCard.showPlaceholder("получается при TLS-рукопожатии",
                "Нажмите «Подключиться» — сервер пришлёт свой сертификат, клиент проверит его по ca.pem "
                + "и сверит имя хоста с subjectAltName.", false);
        console.append(Kind.INFO, "Клиент запущен. Сначала запустите сервер, затем нажмите «Подключиться».");
        reloadCertificates();
        setState(ConnectionState.DISCONNECTED, "Готов к подключению: " + connectionPanel.host() + ":"
                                               + connectionPanel.port());
    }

    // ---------- Сертификаты ----------

    /**
     * Перечитывает client.pem, client.key и ca.pem из выбранной папки и обновляет карточки.
     *
     * @return {@code true}, если набор загружен
     */
    private boolean reloadCertificates() {
        String dir = connectionPanel.certsDirectory().toAbsolutePath().normalize().toString();
        try {
            bundle = CertificateBundle.load(connectionPanel.certsDirectory(), "client");
        } catch (CertificateLoadException e) {
            bundle = null;
            clientCard.showPlaceholder(dir, e.getMessage(), true);
            caCard.showPlaceholder(dir, "Набор сертификатов клиента не загружен.", true);
            console.append(Kind.ERROR, e.getMessage());
            return false;
        }

        boolean signed = bundle.isSignedByCa();
        clientCard.show(bundle.certificate(), "client.pem + client.key",
                List.of(signed ? "✓ подписан ca.pem" : "✕ не подписан ca.pem"));
        caCard.show(bundle.ca(), "ca.pem · им проверяется сервер", List.of("✓ доверенный корень"));
        console.append(Kind.INFO, "Загружен набор сертификатов из " + dir);
        if (!signed) {
            console.append(Kind.ERROR, "client.pem не подписан ca.pem — сервер его, скорее всего, отвергнет");
        }
        // Набор сертификатов дублируется в консоль (stdout), как требует задание
        CertificatePrinter.print(System.out, bundle);
        return true;
    }

    // ---------- Подключение ----------

    /**
     * Connect / Reconnect: закрывает текущее соединение (если есть), заново читает сертификаты и подключается.
     */
    private void connect() {
        boolean reconnect = connection != null;
        closeConnection();
        if (!reloadCertificates()) {
            setState(ConnectionState.FAILED, "Не удалось загрузить сертификаты — см. карточку «Клиент»");
            return;
        }

        String host = connectionPanel.host();
        int port = connectionPanel.port();
        CertificateBundle current = bundle;
        setState(ConnectionState.CONNECTING, (reconnect ? "Переподключение" : "Подключение") + " к " + host + ":"
                                             + port + " — TLS-рукопожатие…");
        console.append(Kind.INFO, (reconnect ? "Переподключение к " : "Подключение к ") + host + ":" + port);

        Async.run(() -> TlsConnection.open(host, port, current, this),
                this::onConnected,
                error -> {
                    String message = ConnectionErrors.describe(error);
                    setState(ConnectionState.FAILED, message);
                    console.append(Kind.ERROR, message);
                });
    }

    private void onConnected(TlsConnection opened) {
        connection = opened;
        SessionInfo session = opened.session();

        serverCard.show(session.serverCertificate(), "получен от " + session.host() + ":" + session.port()
                                                     + " в " + TIME.format(LocalTime.now()),
                List.of("✓ проверен по ca.pem", "✓ имя хоста"));
        sessionPanel.show(session);
        console.setConnected(true);
        console.append(Kind.INFO, "mTLS установлен: " + session.protocol() + ", " + session.cipherSuite()
                                  + ", " + session.handshake().toMillis() + " мс");
        console.append(Kind.RECEIVED, session.welcome());

        CertificatePrinter.print(System.out, "Сервер " + session.host() + ":" + session.port(), session.peerChain());
        setState(ConnectionState.CONNECTED, "Подключено к " + session.host() + ":" + session.port() + " · "
                                            + session.protocol() + " · сервер принял сертификат клиента");
    }

    private void disconnect() {
        closeConnection();
        sessionPanel.clear();
        console.setConnected(false);
        console.append(Kind.INFO, "Отключено пользователем");
        setState(ConnectionState.DISCONNECTED, "Отключено. Сертификат сервера — из последней сессии.");
    }

    /**
     * Вежливо закрывает текущее соединение: сообщает серверу {@code BYE} и закрывает сокет.
     */
    private void closeConnection() {
        if (connection == null) {
            return;
        }
        if (connection.isOpen()) {
            connection.send(Protocol.BYE);
        }
        connection.close();
        connection = null;
    }

    private void send(String command) {
        if (connection == null || !connection.isOpen()) {
            console.append(Kind.ERROR, "Нет соединения");
            return;
        }
        console.append(Kind.SENT, command);
        connection.send(command);
    }

    // ---------- События потока чтения (не EDT!) ----------

    @Override
    public void onMessage(String line) {
        SwingUtilities.invokeLater(() -> console.append(Kind.RECEIVED, line));
    }

    @Override
    public void onClosed(Exception cause) {
        SwingUtilities.invokeLater(() -> {
            connection = null;
            console.setConnected(false);
            sessionPanel.clear();
            String message = cause == null
                    ? "Сервер закрыл соединение"
                    : "Соединение разорвано: " + ConnectionErrors.describe(cause);
            console.append(Kind.ERROR, message);
            setState(ConnectionState.LOST, message + " — нажмите «Переподключиться»");
        });
    }

    // ---------- Оформление ----------

    private void setState(ConnectionState state, String message) {
        this.state = state;
        this.stateMessage = message;
        header.setState(state);
        connectionPanel.setState(state, message);
        setTitle("mTLS-клиент — " + state.title());
    }

    private void toggleTheme() {
        Theme.toggle();
        header.updateThemeButton();
        // Цвета подписей зависят от темы — применяем состояние заново
        setState(state, stateMessage);
    }
}
