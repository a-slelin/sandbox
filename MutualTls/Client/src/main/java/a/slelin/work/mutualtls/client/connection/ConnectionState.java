package a.slelin.work.mutualtls.client.connection;

/**
 * Состояние соединения с сервером, которое показывается в шапке окна.
 */
public enum ConnectionState {

    /** Ещё не подключались или пользователь сам отключился. */
    DISCONNECTED("Не подключено"),

    /** Идёт TCP-подключение и TLS-рукопожатие. */
    CONNECTING("Подключение…"),

    /** Рукопожатие прошло, сервер принял наш сертификат и прислал приветствие. */
    CONNECTED("Подключено · mTLS"),

    /** Подключиться не удалось (сервер недоступен, сертификат отвергнут и т.д.). */
    FAILED("Ошибка подключения"),

    /** Соединение было установлено, но разорвалось (например, сервер остановлен). */
    LOST("Соединение потеряно");

    private final String title;

    ConnectionState(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
