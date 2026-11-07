package a.slelin.work.mutualtls.client.connection;

import a.slelin.work.mutualtls.common.CertificateBundle;
import a.slelin.work.mutualtls.common.Protocol;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Клиентское mTLS-соединение: подключение, рукопожатие, отправка команд и фоновое чтение ответов.
 * <p>
 * Ответы сервера читаются в отдельном виртуальном потоке и передаются в {@link Listener}.
 * Вызовы слушателя происходят <b>не</b> в потоке Swing — интерфейс сам переключается в EDT.
 */
public final class TlsConnection implements AutoCloseable {

    /** Сколько ждать TCP-подключения. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    /** Сколько ждать рукопожатия и приветствия сервера. */
    private static final int HANDSHAKE_TIMEOUT_MS = 10_000;

    /**
     * Получатель событий соединения.
     */
    public interface Listener {

        /** Пришла строка от сервера. */
        void onMessage(String line);

        /**
         * Соединение закрылось не по нашей инициативе.
         *
         * @param cause причина или {@code null}, если сервер закрыл соединение штатно
         */
        void onClosed(Exception cause);
    }

    private final SSLSocket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final SessionInfo session;

    /** Выставляется при закрытии с нашей стороны, чтобы не сообщать о «разрыве». */
    private volatile boolean closing;

    private TlsConnection(SSLSocket socket, BufferedReader in, PrintWriter out, SessionInfo session) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.session = session;
    }

    /**
     * Подключается к серверу и выполняет mTLS-рукопожатие. Метод блокирующий — вызывать в фоне.
     *
     * @param bundle   сертификат и ключ клиента + доверенный CA
     * @param listener получатель сообщений после успешного подключения
     * @throws IOException если сервер недоступен, рукопожатие не удалось или сервер отверг сертификат
     */
    public static TlsConnection open(String host, int port, CertificateBundle bundle, Listener listener)
            throws IOException {
        var factory = bundle.createSslContext().getSocketFactory();
        var socket = (SSLSocket) factory.createSocket();
        try {
            Instant started = Instant.now();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);

            SSLParameters params = socket.getSSLParameters();
            params.setProtocols(Protocol.TLS_PROTOCOLS);
            // Проверка имени хоста: host должен быть в subjectAltName сертификата сервера (как в браузере)
            params.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(params);

            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            // Здесь клиент проверяет сервер по ca.pem и отправляет ему свой client.pem
            socket.startHandshake();

            var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            // В TLS 1.3 сервер проверяет сертификат клиента уже ПОСЛЕ того, как startHandshake() у клиента
            // завершился. Поэтому «подключено» считаем только после приветствия сервера: если сертификат
            // отвергнут, вместо приветствия придёт TLS-alert (bad_certificate / certificate_required)
            String welcome = in.readLine();
            if (welcome == null || !welcome.startsWith(Protocol.WELCOME)) {
                throw new IOException("Сервер закрыл соединение вместо приветствия");
            }
            socket.setSoTimeout(0);

            SSLSession ssl = socket.getSession();
            SessionInfo session = new SessionInfo(host, port, ssl.getProtocol(), ssl.getCipherSuite(),
                    HexFormat.of().withUpperCase().formatHex(ssl.getId()), ssl.getPeerCertificates(),
                    ssl.getLocalCertificates(), Duration.between(started, Instant.now()), Instant.now(), welcome);

            TlsConnection connection = new TlsConnection(socket, in, out, session);
            Thread.ofVirtual().name("mtls-reader").start(() -> connection.readLoop(listener));
            return connection;
        } catch (SocketTimeoutException e) {
            socket.close();
            throw new SocketTimeoutException("Сервер не ответил за " + HANDSHAKE_TIMEOUT_MS / 1000 + " с");
        } catch (IOException | RuntimeException e) {
            socket.close();
            throw e;
        }
    }

    public SessionInfo session() {
        return session;
    }

    /**
     * Отправляет команду серверу одной строкой.
     */
    public void send(String command) {
        out.println(command);
    }

    public boolean isOpen() {
        return !socket.isClosed();
    }

    /**
     * Закрывает соединение по нашей инициативе (TLS close_notify + закрытие сокета).
     */
    @Override
    public void close() {
        closing = true;
        try {
            socket.close();
        } catch (IOException ignored) {
            // Сокет уже закрыт — ничего делать не нужно
        }
    }

    private void readLoop(Listener listener) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                listener.onMessage(line);
            }
            if (!closing) {
                listener.onClosed(null);
            }
        } catch (IOException e) {
            if (!closing) {
                listener.onClosed(e);
            }
        } finally {
            close();
        }
    }
}
