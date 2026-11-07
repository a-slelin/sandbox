package a.slelin.work.mutualtls.client;

import a.slelin.work.mutualtls.common.CertificateBundle;
import a.slelin.work.mutualtls.common.Protocol;

import java.nio.file.Path;

/**
 * Параметры запуска клиента: {@code --host=localhost --port=8443 --certs=MutualTls/certs --dark}.
 * Все значения можно поменять и в окне программы.
 *
 * @param host  хост сервера
 * @param port  порт сервера
 * @param certs папка с client.pem, client.key и ca.pem
 * @param dark  запускаться в тёмной теме
 */
public record ClientConfig(String host, int port, Path certs, boolean dark) {

    public static ClientConfig parse(String[] args) {
        String host = Protocol.DEFAULT_HOST;
        int port = Protocol.DEFAULT_PORT;
        Path certs = CertificateBundle.defaultDirectory();
        boolean dark = false;
        for (String arg : args) {
            if (arg.startsWith("--host=")) {
                host = arg.substring("--host=".length());
            } else if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--certs=")) {
                certs = Path.of(arg.substring("--certs=".length()));
            } else if (arg.equals("--dark")) {
                dark = true;
            } else {
                throw new IllegalArgumentException("Неизвестный аргумент: " + arg
                                                   + " (доступны --host, --port, --certs, --dark)");
            }
        }
        return new ClientConfig(host, port, certs, dark);
    }
}
