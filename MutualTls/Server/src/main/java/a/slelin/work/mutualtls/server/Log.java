package a.slelin.work.mutualtls.server;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Минимальный журнал сервера: время, имя потока и сообщение.
 */
final class Log {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Log() {
    }

    static void info(String message) {
        System.out.printf("%s  %-10s %s%n", TIME.format(LocalTime.now()), tag(), message);
    }

    static void error(String message) {
        System.err.printf("%s  %-10s [ОШИБКА] %s%n", TIME.format(LocalTime.now()), tag(), message);
    }

    private static String tag() {
        String name = Thread.currentThread().getName();
        return name.isEmpty() ? "client" : name;
    }
}
