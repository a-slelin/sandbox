package a.slelin.work.mutualtls.client.connection;

import a.slelin.work.mutualtls.common.CertificateLoadException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Превращает исключения сети и TLS в понятные пользователю сообщения.
 */
public final class ConnectionErrors {

    private ConnectionErrors() {
    }

    public static String describe(Throwable error) {
        String message = String.valueOf(error.getMessage());
        return switch (error) {
            case CertificateLoadException e -> e.getMessage();
            case UnknownHostException e -> "Неизвестный хост: " + message;
            case ConnectException e -> "Сервер недоступен — он запущен? (" + message + ")";
            case SocketTimeoutException e -> "Время ожидания истекло: " + message;
            case SSLException e -> "TLS: " + describeTls(message);
            default -> message;
        };
    }

    /**
     * Самые частые причины неудачного рукопожатия.
     * <p>
     * Сначала проверяются ошибки, которые обнаружил сам клиент (по тексту исключения JSSE),
     * затем — TLS-alert'ы, которые прислал сервер, отказавшись принять клиента.
     */
    private static String describeTls(String message) {
        // --- Клиент сам отверг сертификат сервера ---
        if (message.contains("No subject alternative") || message.contains("No name matching")) {
            return "имя хоста не совпадает с subjectAltName сертификата сервера";
        }
        if (message.contains("PKIX path building") || message.contains("unable to find valid certification path")) {
            return "сертификат сервера не подписан нашим ca.pem";
        }
        if (message.contains("Extended key usage does not permit")) {
            return "у сертификата сервера нет назначения serverAuth (это сертификат не для сервера)";
        }
        if (message.contains("validity check failed") || message.contains("certificate_expired")) {
            return "срок действия сертификата истёк или ещё не наступил";
        }
        // --- Сервер отверг клиента и прислал TLS-alert ---
        if (message.contains("certificate_required")) {
            return "сервер требует сертификат клиента, подписанный его CA, а подходящего сертификата нет";
        }
        if (message.contains("handshake_failure") || message.contains("decrypt_error")) {
            return "сервер отверг рукопожатие: подпись не сходится — закрытый ключ не от этого сертификата?";
        }
        if (message.contains("bad_certificate") || message.contains("certificate_unknown")
            || message.contains("unknown_ca")) {
            return "сервер отверг сертификат клиента (подписан чужим CA или истёк срок) — подробности в журнале сервера";
        }
        return message;
    }
}
