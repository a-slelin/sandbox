package a.slelin.work.mutualtls.common;

/**
 * Не удалось прочитать сертификат или ключ, либо собрать из них {@link javax.net.ssl.SSLContext}.
 * Сообщение рассчитано на показ пользователю.
 */
public class CertificateLoadException extends RuntimeException {

    public CertificateLoadException(String message) {
        super(message);
    }

    public CertificateLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
