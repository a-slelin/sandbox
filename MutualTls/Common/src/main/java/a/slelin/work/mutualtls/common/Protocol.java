package a.slelin.work.mutualtls.common;

/**
 * Настройки по умолчанию и простой текстовый протокол поверх mTLS-канала.
 * <p>
 * Обмен построчный (UTF-8): клиент отправляет команду, сервер отвечает одной строкой.
 * Сразу после рукопожатия сервер сам присылает приветствие {@link #WELCOME}.
 */
public final class Protocol {

    /** Хост по умолчанию: должен совпадать с subjectAltName сертификата сервера. */
    public static final String DEFAULT_HOST = "localhost";

    public static final int DEFAULT_PORT = 8443;

    /** Версии TLS, которые разрешены и серверу, и клиенту. */
    public static final String[] TLS_PROTOCOLS = {"TLSv1.3", "TLSv1.2"};

    public static final String WELCOME = "WELCOME";
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String WHOAMI = "WHOAMI";
    public static final String TIME = "TIME";
    public static final String BYE = "BYE";
    public static final String ECHO = "ECHO";

    private Protocol() {
    }
}
