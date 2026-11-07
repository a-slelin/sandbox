package a.slelin.work.mutualtls.server;

import a.slelin.work.mutualtls.common.CertificateBundle;
import a.slelin.work.mutualtls.common.Protocol;

import java.nio.file.Path;

/**
 * Параметры запуска сервера: {@code --port=8443 --certs=MutualTls/certs}.
 *
 * @param port  порт, на котором слушает сервер
 * @param certs папка с server.pem, server.key и ca.pem
 */
record ServerConfig(int port, Path certs) {

    static ServerConfig parse(String[] args) {
        int port = Protocol.DEFAULT_PORT;
        Path certs = CertificateBundle.defaultDirectory();
        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--certs=")) {
                certs = Path.of(arg.substring("--certs=".length()));
            } else {
                throw new IllegalArgumentException("Неизвестный аргумент: " + arg
                                                   + " (доступны --port=<порт> и --certs=<папка>)");
            }
        }
        return new ServerConfig(port, certs);
    }
}
