package a.slelin.work.mutualtls.server;

import a.slelin.work.mutualtls.common.CertificateInfo;
import a.slelin.work.mutualtls.common.CertificatePrinter;
import a.slelin.work.mutualtls.common.Protocol;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Обслуживание одного клиента: рукопожатие, вывод его сертификата и построчный обмен командами.
 * <p>
 * Команды ({@link Protocol}): {@code PING} → {@code PONG}, {@code WHOAMI} → subject сертификата клиента,
 * {@code TIME} → время сервера, {@code BYE} → закрыть соединение, остальное — эхо.
 */
final class ClientSession implements Runnable {

    private final SSLSocket socket;

    ClientSession(SSLSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String address = socket.getRemoteSocketAddress().toString();
        try (socket) {
            // Явно запускаем рукопожатие: здесь сервер проверяет сертификат клиента по ca.pem
            socket.startHandshake();
            SSLSession session = socket.getSession();
            X509Certificate client = (X509Certificate) session.getPeerCertificates()[0];
            CertificateInfo info = CertificateInfo.of(client);

            Log.info("[OK] рукопожатие с " + address + ": " + session.getProtocol() + ", " + session.getCipherSuite());
            CertificatePrinter.print(System.out, "Клиент " + address, session.getPeerCertificates());

            communicate(info);
            Log.info("клиент " + address + " отключился");
        } catch (SSLHandshakeException e) {
            // Типичные причины: у клиента нет сертификата, он подписан чужим CA или истёк
            Log.error("отказ в рукопожатии с " + address + ": " + e.getMessage());
        } catch (IOException e) {
            Log.error("соединение с " + address + " прервано: " + e.getMessage());
        }
    }

    private void communicate(CertificateInfo client) throws IOException {
        var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

        out.println(Protocol.WELCOME + " " + client.subjectName() + " (" + client.subjectUnit() + ")");

        String line;
        while ((line = in.readLine()) != null) {
            String command = line.strip();
            Log.info("← " + command);
            String reply = switch (command.toUpperCase()) {
                case Protocol.PING -> Protocol.PONG;
                case Protocol.WHOAMI -> "YOU ARE " + client.subjectName() + ", " + client.subjectUnit()
                                        + " · выдан «" + client.issuerName() + "»";
                case Protocol.TIME -> "TIME " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
                case Protocol.BYE -> Protocol.BYE;
                default -> Protocol.ECHO + " " + command;
            };
            out.println(reply);
            Log.info("→ " + reply);
            if (Protocol.BYE.equals(reply)) {
                break;
            }
        }
    }
}
