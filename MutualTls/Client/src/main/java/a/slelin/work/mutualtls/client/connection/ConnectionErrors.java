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
     */
    private static String describeTls(String message) {
        if (message.contains("No subject alternative") || message.contains("No name matching")) {
            return "имя хоста не совпадает с subjectAltName сертификата сервера";
        }
        if (message.contains("PKIX path") || message.contains("unable to find valid certification path")) {
            return "сертификат сервера не подписан нашим ca.pem";
        }
        if (message.contains("certificate_required")) {
            return "сервер требует сертификат клиента, подписанный его CA, а подходящего сертификата нет";
        }
        if (message.contains("bad_certificate") || message.contains("certificate_unknown")
            || message.contains("unknown_ca")) {
            return "сервер отверг сертификат клиента (подписан другим CA?)";
        }
        if (message.contains("certificate_expired") || message.contains("validity check failed")) {
            return "срок действия сертификата истёк";
        }
        return message;
    }
}
