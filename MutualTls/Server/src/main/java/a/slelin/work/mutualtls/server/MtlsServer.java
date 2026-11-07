package a.slelin.work.mutualtls.server;

import a.slelin.work.mutualtls.common.CertificateBundle;
import a.slelin.work.mutualtls.common.CertificateLoadException;
import a.slelin.work.mutualtls.common.CertificatePrinter;
import a.slelin.work.mutualtls.common.Protocol;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Точка входа TLS-сервера с обязательной проверкой клиента (mTLS).
 * <p>
 * Порядок работы:
 * <ol>
 *     <li>загружает {@code server.pem}, {@code server.key}, {@code ca.pem} и печатает их;</li>
 *     <li>открывает {@link SSLServerSocket} с {@code needClientAuth = true} — без сертификата клиента,
 *         подписанного нашим CA, рукопожатие не состоится;</li>
 *     <li>каждое подключение обслуживает {@link ClientSession} в отдельном виртуальном потоке.</li>
 * </ol>
 */
public final class MtlsServer {

    private MtlsServer() {
    }

    public static void main(String[] args) {
        ServerConfig config = ServerConfig.parse(args);

        CertificateBundle bundle;
        try {
            bundle = CertificateBundle.load(config.certs(), "server");
        } catch (CertificateLoadException e) {
            Log.error(e.getMessage());
            System.exit(1);
            return;
        }
        CertificatePrinter.print(System.out, bundle);

        var factory = bundle.createSslContext().getServerSocketFactory();
        try (var server = (SSLServerSocket) factory.createServerSocket(config.port())) {
            server.setEnabledProtocols(Protocol.TLS_PROTOCOLS);
            // Главное отличие mTLS от обычного TLS: клиент ОБЯЗАН предъявить сертификат
            server.setNeedClientAuth(true);

            Log.info("mTLS-сервер слушает порт " + config.port() + " (" + String.join(", ", Protocol.TLS_PROTOCOLS)
                     + "), ожидаю клиентов… Ctrl+C — остановить");

            AtomicInteger counter = new AtomicInteger();
            while (!server.isClosed()) {
                SSLSocket socket = (SSLSocket) server.accept();
                int id = counter.incrementAndGet();
                // Рукопожатие и общение — в своём потоке, чтобы медленный клиент не задерживал остальных
                Thread.ofVirtual().name("client-" + id).start(new ClientSession(socket));
            }
        } catch (IOException e) {
            Log.error("Сервер остановлен: " + e.getMessage());
            System.exit(1);
        }
    }
}
