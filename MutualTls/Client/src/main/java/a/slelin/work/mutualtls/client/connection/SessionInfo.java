package a.slelin.work.mutualtls.client.connection;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;

/**
 * Параметры установленной mTLS-сессии.
 *
 * @param host        к какому хосту подключались (по нему проверялся сертификат сервера)
 * @param port        порт сервера
 * @param protocol    версия TLS, например {@code TLSv1.3}
 * @param cipherSuite набор шифров, например {@code TLS_AES_256_GCM_SHA384}
 * @param sessionId   идентификатор TLS-сессии (HEX)
 * @param peerChain   цепочка сертификатов, которую прислал сервер
 * @param localChain  цепочка, которую мы предъявили серверу
 * @param handshake   длительность подключения и рукопожатия
 * @param connectedAt момент подключения
 * @param welcome     приветствие сервера — подтверждение, что наш сертификат принят
 */
public record SessionInfo(String host, int port, String protocol, String cipherSuite, String sessionId,
                          Certificate[] peerChain, Certificate[] localChain, Duration handshake,
                          Instant connectedAt, String welcome) {

    /** Сертификат самого сервера (первый в цепочке). */
    public X509Certificate serverCertificate() {
        return (X509Certificate) peerChain[0];
    }
}
